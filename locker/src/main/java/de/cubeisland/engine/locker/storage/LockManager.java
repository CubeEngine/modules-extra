/**
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 *
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.cubeisland.engine.locker.storage;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.material.Door;

import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.user.UserManager;
import de.cubeisland.engine.core.util.BlockUtil;
import de.cubeisland.engine.core.world.WorldManager;
import de.cubeisland.engine.locker.BlockLockerConfiguration;
import de.cubeisland.engine.locker.EntityLockerConfiguration;
import de.cubeisland.engine.locker.Locker;
import de.cubeisland.engine.locker.LockerPerm;
import de.cubeisland.engine.locker.commands.CommandListener;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.types.UInteger;

import static de.cubeisland.engine.locker.storage.ProtectedType.getProtectedType;
import static de.cubeisland.engine.locker.storage.TableLockLocations.TABLE_GUARD_LOCATION;
import static de.cubeisland.engine.locker.storage.TableLocks.TABLE_GUARD;

public class LockManager implements Listener
{
    protected final DSLContext dsl;
    protected final Locker module;

    protected WorldManager wm;
    protected UserManager um;

    public final CommandListener commandListener;

    private final Map<Location, Lock> loadedLocks = new HashMap<>();
    private final Map<Chunk, Set<Lock>> loadedLocksInChunk = new HashMap<>();
    private final Map<UUID, Lock> loadedEntityLocks = new HashMap<>();

    public final MessageDigest messageDigest;

    public LockManager(Locker module)
    {
        try
        {
            messageDigest = MessageDigest.getInstance("SHA-1");
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException("SHA-1 hash algorithm not available!");
        }
        this.commandListener = new CommandListener(module, this);
        this.module = module;
        this.wm = module.getCore().getWorldManager();
        this.um = module.getCore().getUserManager();
        this.module.getCore().getEventManager().registerListener(module, this.commandListener);
        this.module.getCore().getEventManager().registerListener(module, this);
        this.dsl = module.getCore().getDB().getDSL();
        for (World world : module.getCore().getWorldManager().getWorlds())
        {
            for (Chunk chunk : world.getLoadedChunks())
            {
                this.loadFromChunk(chunk);
            }
        }
    }

    @EventHandler
    private void onChunkLoad(ChunkLoadEvent event)
    {
        this.loadFromChunk(event.getChunk());
    }

    private void loadFromChunk(Chunk chunk)
    {
        UInteger world_id = UInteger.valueOf(this.wm.getWorldId(chunk.getWorld()));
        Result<LockModel> models = this.dsl.selectFrom(TABLE_GUARD).where(
            TABLE_GUARD.ID.in(this.dsl.select(TABLE_GUARD_LOCATION.GUARD_ID)
                                      .from(TABLE_GUARD_LOCATION)
                                      .where(TABLE_GUARD_LOCATION.WORLD_ID.eq(world_id),
                                             TABLE_GUARD_LOCATION.CHUNKX.eq(chunk.getX()),
                                             TABLE_GUARD_LOCATION.CHUNKZ.eq(chunk.getZ())))).fetch();
        Map<UInteger, Result<LockLocationModel>> locations = this.dsl.selectFrom(TABLE_GUARD_LOCATION)
                                                                     .where(TABLE_GUARD_LOCATION.GUARD_ID.in(models.getValues(TABLE_GUARD.ID)))
                                                                     .fetch().intoGroups(TABLE_GUARD_LOCATION.GUARD_ID);
        for (LockModel model : models)
        {
            Result<LockLocationModel> lockLoc = locations.get(model.getId());
            this.addLoadedLocationLock(new Lock(this, model, lockLoc));
        }
    }

    private void addLoadedLocationLock(Lock lock)
    {
        for (Location loc : lock.getLocations())
        {
            if (loc.getChunk().isLoaded())
            {
                Set<Lock> locks = this.loadedLocksInChunk.get(loc.getChunk());
                if (locks == null)
                {
                    locks = new HashSet<>();
                    this.loadedLocksInChunk.put(loc.getChunk(), locks);
                }
                locks.add(lock);
            }
            this.loadedLocks.put(loc, lock);
        }
    }

    @EventHandler
    private void onChunkUnload(ChunkUnloadEvent event)
    {
        Set<Lock> remove = this.loadedLocksInChunk.remove(event.getChunk());
        if (remove == null) return; // nothing to remove
        for (Lock lock : remove) // remove from chunks
        {
            if (lock.isSingleBlockLock())
            {
                this.loadedLocks.remove(lock.getLocation()); // remove loc
            }
            else
            {
                if (lock.getLocation().getChunk() == lock.getLocation().getChunk()) // same chunk remove both loc
                {
                    this.loadedLocks.remove(lock.getLocation());
                    this.loadedLocks.remove(lock.getLocation2());
                }
                else // different chunks
                {
                    Chunk c1 = lock.getLocation().getChunk();
                    Chunk c2 = lock.getLocation2().getChunk();
                    Chunk chunk = event.getChunk();
                    if ((!c1.isLoaded() && c2 == chunk)
                        ||(!c2.isLoaded() && c1 == chunk))
                    {// Both chunks will be unloaded remove both loc
                        this.loadedLocks.remove(lock.getLocation());
                        this.loadedLocks.remove(lock.getLocation2());
                    }
                    // else the other chunk is still loaded -> do not remove!
                }
            }
            lock.model.update(); // updates if changed (last_access timestamp)
        }
    }

    /**
     * Returns the Lock at given location if the lock there is active
     *
     * @param location the location of the lock
     * @param user the user to get the lock for (can be null)
     * @return the lock or null if there is no lock OR the chunk is not loaded OR the lock is disabled
     */
    public Lock getLockAtLocation(Location location, User user)
    {
        return getLockAtLocation(location, user, true);
    }

    /**
     * Returns the Lock at given Location
     *
     * @param location the location of the lock
     * @param access whether to access the lock or just get information from it
     * @return the lock or null if there is no lock OR the chunk is not loaded
     */
    public Lock getLockAtLocation(Location location, User user, boolean access)
    {
        Lock lock = this.loadedLocks.get(location);
        if (lock != null && access)
        {
            if ((this.module.getConfig().protectWhenOnlyOffline && lock.getOwner().isOnline())
             || (this.module.getConfig().protectWhenOnlyOnline && !lock.getOwner().isOnline()))
            {
                return null;
            }
            lock.model.setLastAccess(new Timestamp(System.currentTimeMillis()));
            if (!lock.validateTypeAt(location))
            {
                lock.delete(user);
                if (user != null)
                {
                    user.sendTranslated("&eDetected invalid BlockProtection is now deleted!");
                }
            }
        }
        return lock;
    }

    /**
     * Returns the Lock for given entityUID
     *
     * @param uniqueId the entities unique id
     * @return the entity-lock or null if there is no lock OR the lock is disabled
     */
    public Lock getLockForEntityUID(UUID uniqueId)
    {
        return this.getLockForEntityUID(uniqueId, true);
    }

    /**
     * Returns the Lock for given entityUID
     *
     * @param uniqueId the entities unique id
     * @param access whether to access the lock or just get information from it
     * @return the entity-lock or null if there is no lock
     */
    public Lock getLockForEntityUID(UUID uniqueId, boolean access)
    {
        Lock lock = this.loadedEntityLocks.get(uniqueId);
        if (lock == null)
        {
            LockModel model = this.dsl.selectFrom(TABLE_GUARD).where(TABLE_GUARD.ENTITY_UID_LEAST.eq(uniqueId.getLeastSignificantBits()),
                                                                      TABLE_GUARD.ENTITY_UID_MOST.eq(uniqueId.getMostSignificantBits())).fetchOne();
            if (model != null)
            {
                lock = new Lock(this, model);
                this.loadedEntityLocks.put(uniqueId, lock);
            }
        }
        if (lock != null && access)
        {
            if ((this.module.getConfig().protectWhenOnlyOffline && lock.getOwner().isOnline())
                || (this.module.getConfig().protectWhenOnlyOnline && !lock.getOwner().isOnline()))
            {
                return null;
            }
            lock.model.setLastAccess(new Timestamp(System.currentTimeMillis()));
        }
        return lock;
    }

    /**
     * Extends a location lock onto an other location
     *
     * @param lock the lock to extend
     * @param location the location to extend to
     */
    public void extendLock(Lock lock, Location location)
    {
        assert this.getLockAtLocation(location, null, false) == null : "Cannot extend Lock onto another!";
        lock.locations.add(location);
        LockLocationModel model = this.dsl.newRecord(TABLE_GUARD_LOCATION).newLocation(lock.model, location);
        model.insert();
        this.loadedLocks.put(location.clone(), lock);
    }

    /**
     * Removes a Lock if the user is authorized or the lock destroyed
     *
     * @param lock the lock to remove
     * @param user the user removing the lock (can be null)
     * @param destroyed true if the Lock is already destroyed
     */
    public void removeLock(Lock lock, User user, boolean destroyed)
    {
        if (destroyed || lock.isOwner(user) || LockerPerm.CMD_REMOVE_OTHER.isAuthorized(user))
        {
            lock.model.delete();
            if (lock.isBlockLock())
            {
                for (Location location : lock.getLocations())
                {
                    this.loadedLocks.remove(location);
                    Set<Lock> locks = this.loadedLocksInChunk.get(location.getChunk());
                    if (locks != null)
                    {
                        locks.remove(lock);
                    }
                }
            }
            else
            {
                this.loadedEntityLocks.remove(lock.model.getUUID());
            }
            if (user != null)
            {
                user.sendTranslated("&aRemoved Lock!");
            }
            return;
        }
        user.sendTranslated("&cThis protection is not yours!");
    }

    /**
     * Creates a new Lock at given Location
     *
     * @param material the material at given location (can missmatch if block is just getting placed)
     * @param location the location to create the lock for
     * @param user the user creating the lock
     * @param lockType the lockType
     * @param password the password
     * @param createKeyBook whether to attempt to create a keyBook
     * @return the created Lock
     */
    public Lock createLock(Material material, Location location, User user, LockType lockType, String password, boolean createKeyBook)
    {
        LockModel model = this.dsl.newRecord(TABLE_GUARD).newLock(user, lockType, getProtectedType(material));
        model.createPassword(this, password).insert();
        List<Location> locations = new ArrayList<>();
        Block block = location.getBlock();
        // Handle MultiBlock Protections
        if (material == Material.CHEST)
        {
            if (block.getState() instanceof Chest && ((Chest)block.getState()).getInventory().getHolder() instanceof DoubleChest)
            {
                DoubleChest dc = (DoubleChest)((Chest)block.getState()).getInventory().getHolder();
                locations.add(((BlockState)dc.getLeftSide()).getLocation());
                locations.add(((BlockState)dc.getRightSide()).getLocation());
            }
        }
        else if (material == Material.WOODEN_DOOR || material == Material.IRON_DOOR_BLOCK)
        {
            locations.add(location);
            if (block.getState().getData() instanceof Door)
            {
                Block botBlock;
                if (((Door)block.getState().getData()).isTopHalf())
                {
                    locations.add(location.clone().add(0, -1, 0));
                    botBlock = locations.get(1).getBlock();
                }
                else
                {
                    botBlock = location.getBlock();
                    locations.add(location.clone().add(0, 1, 0));
                }
                for (BlockFace blockFace : BlockUtil.CARDINAL_DIRECTIONS)
                {
                    if (botBlock.getRelative(blockFace).getType() == block.getType()) // same door type
                    {
                        Door relativeBot = (Door)botBlock.getRelative(blockFace).getState().getData();
                        if (!relativeBot.isTopHalf())
                        {
                            Door botDoor = (Door)botBlock.getState().getData();
                            Door topDoor = (Door)botBlock.getRelative(BlockFace.UP).getState().getData();
                            Door relativeTop = (Door)botBlock.getRelative(blockFace).getRelative(BlockFace.UP).getState().getData();
                            if (botDoor.getFacing() == relativeBot.getFacing() && topDoor.getData() != relativeTop.getData()) // Facing same & opposite hinge
                            {
                                locations.add(botBlock.getRelative(blockFace).getLocation());
                                locations.add(locations.get(2).clone().add(0, 1, 0));
                                break;
                            }
                        } // else ignore
                    }
                }
            }
        }
        if (locations.isEmpty())
        {
            locations.add(location);
        }
        System.out.print(locations.size());
        for (Location loc : locations)
        {
            this.dsl.newRecord(TABLE_GUARD_LOCATION).newLocation(model, loc).insert();
        }
        Lock lock = new Lock(this, model, locations);
        this.addLoadedLocationLock(lock);
        lock.showCreatedMessage(user);
        lock.attemptCreatingKeyBook(user, createKeyBook);
        return lock;
    }

    /**
     * Creates a new Lock for given Entity
     *
     * @param entity the entity to protect
     * @param user user the user creating the lock
     * @param lockType the lockType
     * @param password the password
     * @param createKeyBook whether to attempt to create a keyBook
     * @return the created Lock
     */
    public Lock createLock(Entity entity, User user, LockType lockType, String password, boolean createKeyBook)
    {
        LockModel model = this.dsl.newRecord(TABLE_GUARD).newLock(user, lockType, getProtectedType(entity.getType()), entity.getUniqueId());
        model.createPassword(this, password);
        model.insert();
        Lock lock = new Lock(this, model);
        this.loadedEntityLocks.put(entity.getUniqueId(), lock);
        lock.showCreatedMessage(user);
        lock.attemptCreatingKeyBook(user, createKeyBook);
        return lock;
    }

    public boolean canProtect(Material type)
    {
        for (BlockLockerConfiguration blockprotection : this.module.getConfig().blockprotections)
        {
            if (blockprotection.isType(type))
            {
                return true;
            }
        }
        return false;
    }

    public boolean canProtect(EntityType type)
    {
        for (EntityLockerConfiguration entityProtection : this.module.getConfig().entityProtections)
        {
            if (entityProtection.isType(type))
            {
                return true;
            }
        }
        return false;
    }

    public void saveAll()
    {
        for (Lock lock : this.loadedEntityLocks.values())
        {
            lock.model.update();
        }
        for (Lock lock : this.loadedLocks.values())
        {
            lock.model.update();
        }
    }

    /**
     * Returns the lock for given inventory it exists, also sets the location to the holders location if not null
     *
     * @param inventory
     * @param holderLoc a location object to hold the LockLocation
     * @return the lock for given inventory
     */
    public Lock getLockOfInventory(Inventory inventory, Location holderLoc)
    {
        InventoryHolder holder = inventory.getHolder();
        Lock lock;
        if (holderLoc == null)
        {
            holderLoc = new Location(null, 0, 0, 0);
        }
        if (holder instanceof Entity)
        {
            lock = this.getLockForEntityUID(((Entity)holder).getUniqueId());
            ((Entity)holder).getLocation(holderLoc);
        }
        else
        {
            Location lockLoc;
            if (holder instanceof BlockState)
            {
                lockLoc = ((BlockState)holder).getLocation(holderLoc);
            }
            else if (holder instanceof DoubleChest)
            {
                lockLoc = ((BlockState)((DoubleChest)holder).getRightSide()).getLocation(holderLoc);
            }
            else return null;
            lock = this.getLockAtLocation(lockLoc, null);
        }
        return lock;
    }
}