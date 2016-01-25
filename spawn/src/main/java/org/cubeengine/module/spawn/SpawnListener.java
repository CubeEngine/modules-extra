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
package org.cubeengine.module.spawn;

import org.cubeengine.service.user.User;
import org.cubeengine.service.user.UserManager;
import org.cubeengine.module.core.util.StringUtils;
import org.cubeengine.service.world.WorldManager;
import org.cubeengine.module.roles.Roles;
import de.cubeisland.engine.module.roles.role.RolesAttachment;
import de.cubeisland.engine.module.roles.role.resolved.ResolvedMetadata;
import org.spongepowered.api.world.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

public class SpawnListener implements Listener
{
    private final Roles roles;
    private final WorldManager wm;
    private final UserManager um;

    public SpawnListener(Roles roles)
    {
        this.roles = roles;
        this.wm = roles.getCore().getWorldManager();
        this.um = roles.getCore().getUserManager();
    }

    @EventHandler(priority = EventPriority.HIGH) // has to be called after roles could assign data
    public void onJoin(PlayerJoinEvent event)
    {
        if (!event.getPlayer().hasPlayedBefore())
        {
            User user = um.getExactUser(event.getPlayer().getUniqueId());
            RolesAttachment rolesAttachment = user.get(RolesAttachment.class);
            if (rolesAttachment == null)
            {
                this.roles.getLog().warn("Missing RolesAttachment!");
                return;
            }
            String spawnString = rolesAttachment.getCurrentMetadataString("rolespawn");
            if (spawnString != null)
            {
                Location spawnLoc = this.getSpawnLocation(spawnString);
                if (spawnLoc == null)
                {
                    roles.getLog().warn("Invalid Location. Check your role configuration!");
                    return;
                }
                user.teleport(spawnLoc.add(0.5,0,0.5), TeleportCause.PLUGIN);
            }
        }
    }

    @EventHandler
    public void onSpawn(PlayerRespawnEvent event)
    {
        if (!event.isBedSpawn())
        {
            User user = um.getExactUser(event.getPlayer().getUniqueId());
            RolesAttachment rolesAttachment = user.get(RolesAttachment.class);
            if (rolesAttachment == null)
            {
                this.roles.getLog().warn("Missing RolesAttachment!");
                return;
            }
            ResolvedMetadata roleSpawnMeta = rolesAttachment.getDataHolder(event.getRespawnLocation().getWorld()).getMetadata().get("rolespawn");
            if (roleSpawnMeta != null && roleSpawnMeta.getValue() != null)
            {
                Location spawnLoc = this.getSpawnLocation(roleSpawnMeta.getValue());
                if (spawnLoc == null)
                {
                    roles.getLog().warn("Invalid Location. Check your role configuration!");
                    return;
                }
                event.setRespawnLocation(spawnLoc.add(0.5,0,0.5));
            }
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
            World world = this.wm.getWorld(spawnStrings[5]);
            return new Location(world,x,y,z,yaw, pitch);
        }
        catch (Exception ex)
        {
            return null;
        }
    }
}
