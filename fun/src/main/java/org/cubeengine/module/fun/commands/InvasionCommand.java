/*
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
package org.cubeengine.module.fun.commands;

import java.util.Optional;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.libcube.util.CauseUtil;
import org.cubeengine.module.fun.Fun;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.formatter.MessageType;
import org.cubeengine.libcube.service.matcher.EntityMatcher;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class InvasionCommand
{
    private final Fun module;
    private I18n i18n;
    private EntityMatcher em;

    public InvasionCommand(Fun module, I18n i18n, EntityMatcher entityMatcher)
    {
        this.module = module;
        this.i18n = i18n;
        this.em = entityMatcher;
    }

    @Command(desc = "Spawns a mob next to every player on the server")
    public void invasion(CommandSource context, String mob)
    {
        EntityType entityType = em.mob(mob, context.getLocale());
        if (entityType == null)
        {
            i18n.send(context, MessageType.NEGATIVE, "EntityType {input} not found", mob);
            return;
        }
        for (Player player : Sponge.getServer().getOnlinePlayers())
        {
            Optional<BlockRayHit<World>> end =
                BlockRay.from(player).stopFilter(BlockRay.onlyAirFilter())
                    .distanceLimit(module.getConfig().command.invasion.distance).build().end();
            if (end.isPresent())
            {
                Location<World> location = end.get().getLocation();
                Entity entity = location.getExtent().createEntity(entityType, location.getPosition());
                location.getExtent().spawnEntity(entity, CauseUtil.spawnCause(context));
            }
        }
    }
}
