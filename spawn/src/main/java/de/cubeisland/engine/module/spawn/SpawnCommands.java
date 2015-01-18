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
package de.cubeisland.engine.module.spawn;

import de.cubeisland.engine.command.parametric.Command;
import de.cubeisland.engine.command.parametric.Flag;
import de.cubeisland.engine.command.parametric.Complete;
import de.cubeisland.engine.command.parametric.Default;
import de.cubeisland.engine.command.parametric.Named;
import de.cubeisland.engine.command.parametric.Optional;
import de.cubeisland.engine.command.parameter.TooFewArgumentsException;
import de.cubeisland.engine.core.command.CommandSender;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.StringUtils;
import de.cubeisland.engine.core.util.math.BlockVector3;
import de.cubeisland.engine.module.roles.RoleCompleter;
import de.cubeisland.engine.module.roles.Roles;
import de.cubeisland.engine.module.roles.role.ResolvedDataHolder;
import de.cubeisland.engine.module.roles.role.Role;
import de.cubeisland.engine.module.roles.role.RolesAttachment;
import de.cubeisland.engine.module.roles.role.RolesManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import static de.cubeisland.engine.core.util.formatter.MessageType.*;

public class SpawnCommands
{

    private final Roles roles;
    private final Spawn module;

    private RolesManager manager;

    public SpawnCommands(Roles roles, Spawn module)
    {
        this.roles = roles;
        this.module = module;
        manager = roles.getRolesManager();
    }

    @Command(desc = "Changes the respawnpoint")
    public void setRoleSpawn(CommandSender context, @Complete(RoleCompleter.class) String role, @Default World world, @Optional Double x, @Optional Double y, @Optional Double z)
    {
        float yaw = 0;
        float pitch = 0;
        if (z == null)
        {
            if (!(context instanceof User))
            {
                throw new TooFewArgumentsException();
            }
            final Location loc = ((User)context).getLocation();
            x = loc.getX();
            y = loc.getY();
            z = loc.getZ();
            yaw = loc.getYaw();
            pitch = loc.getPitch();
        }
        Role r = manager.getProvider(world).getRole(role);
        if (r == null)
        {
            context.sendTranslated(NEGATIVE, "Could not find the role {input} in {world}!", role, world);
            return;
        }
        setRoleSpawn(world, x, y, z, yaw, pitch, r);
        context.sendTranslated(POSITIVE, "The spawn in {world} for the role {name#role} is now set to {vector}", world, r.getName(), new BlockVector3(x.intValue(),y.intValue(),z.intValue()));
    }

    private void setRoleSpawn(World world, Double x, Double y, Double z, float yaw, float pitch, Role role)
    {
        String[] locStrings = new String[6];
        locStrings[0] = String.valueOf(x.intValue());
        locStrings[1] = String.valueOf(y.intValue());
        locStrings[2] = String.valueOf(z.intValue());
        locStrings[3] = String.valueOf(yaw);
        locStrings[4] = String.valueOf(pitch);
        locStrings[5] = world.getName();
        role.setMetadata("rolespawn", StringUtils.implode(":", locStrings));
        role.save();
        manager.getProvider(world).recalculateRoles();
    }

    // TODO teleport all players to spawn

    @Command(desc = "Teleports a player to spawn (of a role)")
    public void spawn(CommandSender context, @Default User player, @Optional World world,
                      @Named({"role", "r"}) @Complete(RoleCompleter.class) String role, // TODO Role Reader & DefaultProvider
                      @Flag boolean force, @Flag boolean worldSpawn)
    {
        if (world == null)
        {
            world = module.getConfiguration().mainWorld.getWorld();
            if (world == null)
            {
                module.getLog().warn("Unknown main world configured!");
                world = player.getWorld();
            }
        }

        if (worldSpawn)
        {
            Location spawnLocation = world.getSpawnLocation().add(0.5, 0, 0.5);
            Location playerLocation = player.getLocation();
            spawnLocation.setPitch(playerLocation.getPitch());
            spawnLocation.setYaw(playerLocation.getYaw());
            if (!this.tpTo(player, spawnLocation, force))
            {
                context.sendTranslated(NEGATIVE, "Teleport failed!");
                return;
            }
            context.sendTranslated(POSITIVE, "You are now standing at the spawn in {world}!", world);
            return;
        }

        ResolvedDataHolder r;
        String roleSpawn;
        if (role != null)
        {
            r = manager.getProvider(world).getRole(role);
            if (r == null)
            {
                context.sendTranslated(NEGATIVE, "Could not find the role {input} in {world}!", role, world);
                return;
            }
        }
        else
        {
            RolesAttachment rolesAttachment = player.get(RolesAttachment.class);
            if (rolesAttachment == null)
            {
                this.roles.getLog().warn("Missing RolesAttachment!");
                return;
            }
            r = rolesAttachment.getDataHolder(world);
        }
        roleSpawn = r.getMetadata().get("rolespawn").getValue();
        if (roleSpawn == null)
        {
            context.sendTranslated(NEGATIVE, "No spawn point for {name} in {world}!", r.getName(), world);
            return;
        }
        Location spawnLocation = this.getSpawnLocation(roleSpawn);
        if (spawnLocation == null)
        {
            context.sendTranslated(CRITICAL, "Invalid spawn location for {name} in {world}!", r.getName(), world);
            context.sendMessage(roleSpawn);
        }

        force = force && module.perms().COMMAND_SPAWN_FORCE.isAuthorized(context);

        if (!player.isOnline())
        {
            context.sendTranslated(NEGATIVE, "You cannot teleport an offline player to spawn!");
            return;
        }
        if (!context.equals(player) && !force && module.perms().COMMAND_SPAWN_PREVENT.isAuthorized(player))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to spawn {user}!", player);
            return;
        }
        if (!this.tpTo(player, spawnLocation, force))
        {
            context.sendTranslated(NEGATIVE, "Teleport failed!");
            return;
        }
        if (!context.equals(player))
        {
            context.sendTranslated(POSITIVE, "Teleported {user} to the spawn of the role {name#role} in {world}", player, r.getName(), world);
        }
        else if (role == null)
        {
            context.sendTranslated(POSITIVE, "You are now standing at your role's spawn in {world}!", world);
        }
        else
        {
            context.sendTranslated(POSITIVE, "You are now standing at the spawn of {name#role} in {world}!", r.getName(), world);
        }
    }

    private Location getSpawnLocation(String value)
    {
        try
        {
            String[] spawnStrings = StringUtils.explode(":",value);
            int x = Integer.valueOf(spawnStrings[0]);
            int y = Integer.valueOf(spawnStrings[1]);
            int z = Integer.valueOf(spawnStrings[2]);
            float yaw = Float.valueOf(spawnStrings[3]);
            float pitch = Float.valueOf(spawnStrings[4]);
            World world = this.module.getCore().getWorldManager().getWorld(spawnStrings[5]);
            return new Location(world,x,y,z,yaw, pitch);
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    private boolean tpTo(User user, Location location, boolean force)
    {
        if (force)
        {
            return user.teleport(location, TeleportCause.COMMAND);
        }
        else
        {
            return user.safeTeleport(location,TeleportCause.COMMAND,false);
        }
    }
}
