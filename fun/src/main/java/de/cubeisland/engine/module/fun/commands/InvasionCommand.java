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
package de.cubeisland.engine.module.fun.commands;

import java.util.Collections;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.service.command.CommandContext;
import de.cubeisland.engine.module.core.util.matcher.Match;
import de.cubeisland.engine.module.fun.Fun;
import org.bukkit.Bukkit;
import org.spongepowered.api.world.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.spongepowered.api.entity.player.Player;

import org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;

public class InvasionCommand
{
    private final Fun module;

    public InvasionCommand(Fun module)
    {
        this.module = module;
    }

    @Command(desc = "Spawns a mob next to every player on the server")
    public void invasion(CommandContext context, String mob)
    {
        EntityType entityType = Match.entity().mob(mob);
        if (entityType == null)
        {
            context.sendTranslated(NEGATIVE, "EntityType {input} not found", mob);
            return;
        }
        final Location helperLocation = new Location(null, 0, 0, 0);
        for (Player player : Bukkit.getOnlinePlayers())
        {
            Location location = player.getTargetBlock(Collections.<Material>emptySet(), this.module.getConfig().command.invasion.distance).getLocation(
                helperLocation);
            if (location.getBlock().getType() != Material.AIR)
            {
                location = location.clone();
                location.subtract(player.getLocation(helperLocation).getDirection().multiply(2));
            }
            player.getWorld().spawnEntity(location, entityType);
        }
    }
}
