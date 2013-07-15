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
package de.cubeisland.cubeengine.guests.prevention.preventions;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import de.cubeisland.cubeengine.core.bukkit.BukkitCore;
import de.cubeisland.cubeengine.core.util.math.Square;
import de.cubeisland.cubeengine.core.util.math.Vector2;
import de.cubeisland.cubeengine.guests.Guests;
import de.cubeisland.cubeengine.guests.prevention.Prevention;

/**
 * Prevents movement.
 */
public class MovePrevention extends Prevention
{
    private int width;
    private final Server server;

    public MovePrevention(Guests guests)
    {
        super("move", guests, false);
        setThrottleDelay(3);
        this.server = ((BukkitCore)guests.getCore()).getServer();
    }

    @Override
    public String getConfigHeader()
    {
        return super.getConfigHeader() + "\n"
            + "Configuration info:\n"
            + "    width: the number of blocks a player can move awy from the spawn\n";
    }

    @Override
    public void enable()
    {
        super.enable();
        this.width = getConfig().getInt("width");
    }

    @Override
    public Configuration getDefaultConfig()
    {
        Configuration config = super.getDefaultConfig();

        config.set("width", Math.max(5, this.server.getSpawnRadius()));

        return config;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void move(PlayerMoveEvent event)
    {
        final Location from = event.getFrom();
        final Location to = event.getTo();

        // only check if the player really moved
        if (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ())
        {
            return;
        }

        final Player player = event.getPlayer();
        if (!can(player))
        {
            Location spawnLocation = player.getWorld().getSpawnLocation();
            // create a square around the spawn
            final Square spawnSquare = new Square(
                new Vector2(spawnLocation.getBlockX() - this.width, spawnLocation.getBlockZ() - this.width),
                this.width * 2);

            // is the new location inside the spawn square?
            if (!spawnSquare.contains(new Vector2(to.getBlockX(), to.getBlockZ())))
            {
                Location fallback = from;
                if (!spawnSquare.contains(new Vector2(fallback.getBlockX(), fallback.getBlockZ())))
                {
                    fallback = player.getWorld().getSpawnLocation();
                }
                sendMessage(player);
                player.teleport(fallback, PlayerTeleportEvent.TeleportCause.PLUGIN);
                event.setCancelled(true);
            }
        }
    }
}
