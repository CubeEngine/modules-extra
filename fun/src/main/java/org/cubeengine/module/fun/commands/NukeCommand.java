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

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Flag;
import org.cubeengine.libcube.service.command.annotation.Named;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.math.shape.Cuboid;
import org.cubeengine.libcube.util.math.shape.Shape;
import org.cubeengine.module.fun.Fun;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.explosive.fused.PrimedTNT;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.util.blockray.RayTrace;
import org.spongepowered.api.util.blockray.RayTraceResult;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.explosion.Explosion;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

public class NukeCommand
{
    private final Fun module;
    private final NukeListener nukeListener;
    private final I18n i18n;

    public NukeCommand(Fun module, I18n i18n, EventManager em)
    {
        this.module = module;
        this.i18n = i18n;
        this.nukeListener = new NukeListener();
        em.registerListener(Fun.class, this.nukeListener);
    }

    @Command(desc = "Makes a carpet of TNT fall on a player or where you're looking")
    public void nuke(CommandCause context,
                     @Option Integer diameter,
                     @Named({"player", "p"}) Player player,
                     @Named({"height", "h"}) Integer height,
                     @Named({"range", "r"}) Integer range,
                     @Flag boolean unsafe,
                     @Flag boolean quiet)
    {
        ServerLocation location;
        range = range == null ? 4 : range;
        height = height == null ? 5 : height;

        diameter = diameter == null ? 5 : diameter;
        diameter = Math.min(10, diameter);

        if(range != 4 && !context.hasPermission(module.perms().COMMAND_NUKE_CHANGE_RANGE.getId()))
        {
            i18n.send(context, NEGATIVE, "You are not allowed to change the explosion range of the nuke carpet!");
            return;
        }
        if(range < 0 || range > this.module.getConfig().command.nuke.maxExplosionRange)
        {
            i18n.send(context, NEGATIVE, "The explosion range can't be less than 0 or greater than {integer}", this.module.getConfig().command.nuke.maxExplosionRange);
            return;
        }

        if(player != null)
        {
            if (!context.getSubject().equals(player) && !context.hasPermission(module.perms().COMMAND_NUKE_OTHER.getId()))
            {
                i18n.send(context, NEGATIVE, "You are not allowed to specify a player!");
                return;
            }
            location = ((ServerPlayer)context).getServerLocation();
        }
        else
        {
            if(!(context instanceof ServerPlayer))
            {
                i18n.send(context, NEGATIVE, "This command can only be used by a player!");
                return;
            }
            final Optional<RayTraceResult<LocatableBlock>> end = RayTrace.block().sourceEyePosition(((ServerPlayer)context.getSubject())).select(RayTrace.onlyAir()).limit(100).execute();
            if (!end.isPresent())
            {
                throw new IllegalStateException();
            }
            location = end.get().getSelectedObject().getServerLocation();
        }

        location = this.getSpawnLocation(location, height);
        Shape aShape = new Cuboid(new Vector3d(location.getX() + .5, location.getY() + .5, location.getZ()+ .5) , diameter, 1, diameter);
        int blockAmount = this.spawnNuke(aShape, location.getWorld(), range, unsafe);

        if(!quiet)
        {
            i18n.send(context, POSITIVE, "You spawned {integer} blocks of tnt.", blockAmount);
        }
    }

    private ServerLocation getSpawnLocation(ServerLocation location, int height)
    {
        int noBlock = 0;
        while (noBlock != Math.abs(height))
        {
            location.add(0, height > 0 ? 1 : -1, 0);
            if (location.getBlock().getType() == BlockTypes.AIR.get())
            {
                noBlock++;
            }
            else
            {
                noBlock = 0;
            }
        }
        return location;
    }

    /**
     * iterates through the points of the shape and spawns a tnt block at the positions.
     *
     * @return the number of spawned tnt blocks.
     */
    public int spawnNuke(Shape shape, ServerWorld world, int range, boolean unsafe)
    {
        int numberOfBlocks = 0;
        for (Vector3d vector : shape)
        {
            PrimedTNT tnt = (PrimedTNT)world.createEntity(EntityTypes.TNT, vector.clone());
            tnt.offer(Keys.VELOCITY, new Vector3d(0, 0, 0));
            tnt.offer(Keys.EXPLOSION_RADIUS, range);
            // TODO push cause
            world.spawnEntity(tnt);

            numberOfBlocks++;

            if (!unsafe)
            {
                this.nukeListener.add(tnt);
            }

            if(numberOfBlocks >= this.module.getConfig().command.nuke.maxTNTAmount)
            {
                return numberOfBlocks;
            }
        }
        return numberOfBlocks;
    }

    public static class NukeListener
    {
        private final Set<PrimedTNT> noBlockDamageSet = new HashSet<>();

        public void add(PrimedTNT tnt)
        {
            this.noBlockDamageSet.add(tnt);
        }

        @Listener
        public void onEntityExplode(final ExplosionEvent.Pre event, @First PrimedTNT cause)
        {
            if (noBlockDamageSet.contains(cause))
            {
                noBlockDamageSet.remove(cause);
                event.setExplosion(Explosion.builder().from(event.getExplosion())
                                            .shouldBreakBlocks(false)
                                            .shouldDamageEntities(false).build());
            }
        }
    }
}
