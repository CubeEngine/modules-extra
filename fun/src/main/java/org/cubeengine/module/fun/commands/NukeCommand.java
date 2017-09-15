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

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.entity.EntityTypes.PRIMED_TNT;
import static org.spongepowered.api.util.blockray.BlockRay.onlyAirFilter;

import com.flowpowered.math.vector.Vector3d;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.math.shape.Cuboid;
import org.cubeengine.libcube.util.math.shape.Shape;
import org.cubeengine.module.fun.Fun;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.explosive.PrimedTNT;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.explosion.Explosion;

import java.util.HashSet;
import java.util.Set;

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
    public void nuke(CommandSource context,
                     @Optional Integer diameter,
                     @Named({"player", "p"}) Player player,
                     @Named({"height", "h"}) Integer height,
                     @Named({"range", "r"}) Integer range,
                     @Flag boolean unsafe,
                     @Flag boolean quiet)
    {
        Location<World> location;
        range = range == null ? 4 : range;
        height = height == null ? 5 : height;

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
            if (!context.equals(player) && !context.hasPermission(module.perms().COMMAND_NUKE_OTHER.getId()))
            {
                i18n.send(context, NEGATIVE, "You are not allowed to specify a player!");
                return;
            }
            location = ((Player)context).getLocation();
        }
        else
        {
            if(!(context instanceof Player))
            {
                i18n.send(context, NEGATIVE, "This command can only be used by a player!");
                return;
            }
            java.util.Optional<BlockRayHit<World>> end = BlockRay.from(((Player)context)).stopFilter(onlyAirFilter()).distanceLimit(100).build().end();
            if (!end.isPresent())
            {
                throw new IllegalStateException();
            }
            location = end.get().getLocation();
        }

        location = this.getSpawnLocation(location, height);
        Shape aShape = new Cuboid(new Vector3d(location.getX() + .5, location.getY() + .5, location.getZ()+ .5) , diameter, 1, diameter);
        int blockAmount = this.spawnNuke(aShape, location.getExtent(), range, unsafe);

        if(!quiet)
        {
            i18n.send(context, POSITIVE, "You spawned {integer} blocks of tnt.", blockAmount);
        }
    }

    private Location<World> getSpawnLocation(Location<World> location, int height)
    {
        int noBlock = 0;
        while (noBlock != Math.abs(height))
        {
            location.add(0, height > 0 ? 1 : -1, 0);
            if (location.getBlock().getType() == BlockTypes.AIR)
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
    public int spawnNuke(Shape shape, World world, int range, boolean unsafe)
    {
        int numberOfBlocks = 0;
        for (Vector3d vector : shape)
        {
            PrimedTNT tnt = (PrimedTNT)world.createEntity(PRIMED_TNT, vector.clone());
            tnt.setVelocity(new Vector3d(0,0,0));
            tnt.offer(Keys.EXPLOSION_RADIUS, java.util.Optional.of(range));
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
