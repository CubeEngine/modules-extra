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
package org.cubeengine.module.itemrepair.repair;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.cubeengine.libcube.service.database.Database;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.libcube.util.CauseUtil;
import org.cubeengine.module.itemrepair.Itemrepair;
import org.cubeengine.module.itemrepair.material.RepairItemContainer;
import org.cubeengine.module.itemrepair.repair.blocks.RepairBlock;
import org.cubeengine.module.itemrepair.repair.blocks.RepairBlock.RepairBlockInventory;
import org.cubeengine.module.itemrepair.repair.blocks.RepairBlockConfig;
import org.cubeengine.module.itemrepair.repair.storage.RepairBlockModel;
import org.cubeengine.module.itemrepair.repair.storage.RepairBlockPersister;
import org.jooq.DSLContext;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.world.LoadWorldEvent;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.module.itemrepair.repair.storage.TableRepairBlock.TABLE_REPAIR_BLOCK;

public class RepairBlockManager
{
    private final Map<BlockType, RepairBlock> repairBlocks;
    private final Map<Location<World>, BlockType> blockMap;
    private final RepairBlockPersister persister;

    protected final Itemrepair module;
    private final RepairItemContainer itemProvider;

    private final DSLContext dsl;

    public RepairBlockManager(Itemrepair module, Database db, EventManager em, I18n i18n, EconomyService economy,
                              PermissionManager pm)
    {
        this.dsl = db.getDSL();
        this.module = module;
        this.repairBlocks = new HashMap<>();
        this.itemProvider = new RepairItemContainer(module.getConfig().price.baseMaterials);

        for (Entry<String, RepairBlockConfig> entry : module.getConfig().repairBlockConfigs.entrySet())
        {
            RepairBlock repairBlock = new RepairBlock(module, this, entry.getKey(), entry.getValue(), pm, economy, i18n);
            this.addRepairBlock(repairBlock);
        }
        this.blockMap = new HashMap<>();
        this.persister = new RepairBlockPersister(module, db);
        em.registerListener(Itemrepair.class, this);
        for (World world : Sponge.getServer().getWorlds())
        {
            this.loadRepairBlocks(this.persister.getAll(world));
        }
    }

    private void loadRepairBlocks(Collection<RepairBlockModel> models)
    {
        for (RepairBlockModel model : models)
        {
            Location<World> block = model.getBlock();
            if (block.getBlockType().getName().equals(model.getValue(TABLE_REPAIR_BLOCK.TYPE)))
            {
                if (this.repairBlocks.containsKey(block.getBlockType()))
                {
                    this.blockMap.put(block,block.getBlockType());
                }
                else
                {
                    this.module.getLog().info("Deleting saved RepairBlock that is no longer a RepairBlock at {}:{}:{} in {}",
                                              block.getX(), block.getY(), block.getZ(), block.getExtent().getName());
                    model.deleteAsync();
                }
            }
            else
            {
                this.module.getLog().info("Deleting saved RepairBlock that does not correspond to block at {}:{}:{} in {}",
                                          block.getX(), block.getY(), block.getZ(), block.getExtent().getName());
                model.deleteAsync();
            }
        }
    }

    @Listener
    public void onWorldLoad(LoadWorldEvent event)
    {
        this.loadRepairBlocks(this.persister.getAll(event.getTargetWorld()));
    }

    /**
     * Adds a repair block
     *
     * @param block the repair block
     * @return fluent interface
     */
    public RepairBlockManager addRepairBlock(RepairBlock block)
    {
        this.repairBlocks.put(block.getMaterial(), block);
        this.module.getLog().debug("Added a repair block: {} on ID: {}", block.getName(), block.getMaterial());
        return this;
    }

    /**
     * Returns a repair block by it's materials
     *
     * @param material the material
     * @return the repair block
     */
    public RepairBlock getRepairBlock(BlockType material)
    {
        return this.repairBlocks.get(material);
    }

    /**
     * Returns the attached repair block of a block
     *
     * @param block the block
     * @return the attached repair block
     */
    public RepairBlock getRepairBlock(Location<World> block)
    {
        BlockType repairBlockMaterial = this.blockMap.get(block);
        if (repairBlockMaterial != null)
        {
            return this.getRepairBlock(repairBlockMaterial);
        }
        return null;
    }

    /**
     * Checks whether the given block is a repair block
     *
     * @param block the block to check
     * @return true if it is one
     */
    public boolean isRepairBlock(Location<World> block)
    {
        return this.blockMap.containsKey(block);
    }

    /**
     * Attaches a repair block to a block
     *
     * @param block the block to attach to
     * @return true on success
     */
    public boolean attachRepairBlock(Location<World> block)
    {
        BlockType material = block.getBlockType();
        if (!this.isRepairBlock(block))
        {
            if (this.repairBlocks.containsKey(material))
            {
                this.blockMap.put(block, material);
                this.persister.storeBlock(block, this.dsl.newRecord(TABLE_REPAIR_BLOCK).newRepairBlock(block));
                return true;
            }
        }
        return false;
    }

    /**
     * Detaches a repair block from a block
     *
     * @param block the block to detach from
     * @return true on success
     */
    public boolean detachRepairBlock(Location<World> block)
    {
        if (this.isRepairBlock(block))
        {
            this.blockMap.remove(block);
            this.persister.deleteByBlock(block);
            return true;
        }
        return false;
    }

    public void removePlayer(final Player player)
    {
        if (player == null)
        {
            return;
        }
        RepairBlockInventory inventory;
        for (RepairBlock repairBlock : this.repairBlocks.values())
        {
            inventory = repairBlock.removeInventory(player);
            if (inventory != null)
            {
                final World world = player.getWorld();
                final Location loc = player.getLocation();
                for (Inventory slot : inventory.inventory)
                {
                    if (slot.peek().isPresent())
                    {
                        Entity drop = world.createEntity(EntityTypes.ITEM, loc.getPosition());
                        drop.offer(Keys.REPRESENTED_ITEM, slot.peek().get().createSnapshot());
                        world.spawnEntity(drop, CauseUtil.spawnCause(player));
                    }
                }
            }
        }
    }

    public RepairItemContainer getItemProvider()
    {
        return itemProvider;
    }
}
