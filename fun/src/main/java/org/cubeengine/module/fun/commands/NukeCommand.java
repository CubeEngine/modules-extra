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
package org.cubeengine.module.fun.commands;

import java.util.HashSet;
import com.flowpowered.math.vector.Vector3d;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.libcube.util.math.Vector3;
import org.cubeengine.libcube.util.math.shape.Cuboid;
import org.cubeengine.libcube.util.math.shape.Cylinder;
import org.cubeengine.libcube.util.math.shape.Shape;
import org.cubeengine.libcube.util.math.shape.Sphere;
import org.cubeengine.module.fun.Fun;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.explosive.PrimedTNT;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.entity.EntityTypes.PRIMED_TNT;
import static org.spongepowered.api.util.blockray.BlockRay.onlyAirFilter;

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
                     @Optional Integer param1,
                     @Optional Integer param2,
                     @Optional Integer param3,
                     @Named({"player", "p"}) Player player,
                     @Named({"height", "h"}) Integer height,
                     @Named({"range", "r"}) Integer range,
                     @Named({"shape", "s"}) String shape,
                     @Flag boolean unsafe,
                     @Flag boolean quiet)
    {
        Location<World> location;
        range = range == null ? 4 : range;
        height = height == null ? 5 : height;

        if(range != 4 && !context.hasPermission(module.perms().COMMAND_NUKE_CHANGE_RANGE.getId()))
        {
            i18n.sendTranslated(context, NEGATIVE, "You are not allowed to change the explosion range of the nuke carpet!");
            return;
        }
        if(range < 0 || range > this.module.getConfig().command.nuke.maxExplosionRange)
        {
            i18n.sendTranslated(context, NEGATIVE, "The explosion range can't be less than 0 or greater than {integer}", this.module.getConfig().command.nuke.maxExplosionRange);
            return;
        }

        if(player != null)
        {
            if (!context.equals(player) && !context.hasPermission(module.perms().COMMAND_NUKE_OTHER.getId()))
            {
                i18n.sendTranslated(context, NEGATIVE, "You are not allowed to specify a player!");
                return;
            }
            location = ((Player)context).getLocation();
        }
        else
        {
            if(!(context instanceof Player))
            {
                i18n.sendTranslated(context, NEGATIVE, "This command can only be used by a player!");
                return;
            }
            java.util.Optional<BlockRayHit<World>> end = BlockRay.from(((Player)context)).filter(onlyAirFilter()).blockLimit(100).build().end();
            if (!end.isPresent())
            {
                throw new IllegalStateException();
            }
            location = end.get().getLocation();
        }

        Shape aShape = this.getShape(context, shape, location, height, param1, param2, param3);
        if(aShape == null)
        {
            return;
        }

        int blockAmount = this.spawnNuke(aShape, location.getExtent(), range, unsafe);

        if(!quiet)
        {
            i18n.sendTranslated(context, POSITIVE, "You spawned {integer} blocks of tnt.", blockAmount);
        }
    }

    private Shape getShape(CommandSource context, String shape, Location location, int locationHeight, Integer param1,
                           Integer param2, Integer param3)
    {
        shape = shape == null ? "cylinder" : shape;

        switch (shape)
        {
        case "cylinder":
            location = this.getSpawnLocation(location, locationHeight);
            int radiusX = param1 == null ? 1 : param1;
            return new Cylinder(new Vector3(location.getX(), location.getY(), location.getZ()), radiusX,
                        param3 == null ? radiusX : param2, radiusX);
        case "cube":
        case "cuboid":
            int width = param1 == null ? 1 : param1;
            int height = shape.equals("cube") ? width : param2 == null ? width : param2;
            int depth = shape.equals("cube") ? width : param3 == null ? width : param3;

            location = location.add(- width / 2d, 0, - depth / 2d);
            location = this.getSpawnLocation(location, locationHeight);
            return new Cuboid(new Vector3(location.getX(), location.getY(), location.getZ()), width, height, depth);
        case "sphere":
            int radius = param1 == null ? 1 : param1;
            location = this.getSpawnLocation(location, locationHeight);
            return new Sphere(new Vector3(location.getX(), location.getY(), location.getZ()), radius);
        default:
            i18n.sendTranslated(context, NEGATIVE, "The shape {input} was not found!", shape);
            break;
        }
        return null;
    }

    private Location getSpawnLocation(Location location, int height)
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
        for (Vector3 vector : shape)
        {
            PrimedTNT entity = (PrimedTNT)world.createEntity(PRIMED_TNT, new Vector3d(vector.x, vector.y, vector.z)).get();
            entity.setVelocity(new Vector3d(0,0,0));
            entity.set
            world.spawnEntity(entity, Cause.of(NamedCause.source(this))); // TODO cause

            TNTPrimed tnt = world.spawn(new Location(world, vector.x, vector.y, vector.z), TNTPrimed.class);
            tnt.setVelocity(new Vector(0, 0, 0));
            tnt.setYield(range);

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

    private class NukeListener
    {
        private final Set<TNTPrimed> noBlockDamageSet;
        private final TaskManager taskManager;
        private int taskID;

        public NukeListener()
        {
            this.noBlockDamageSet = new HashSet<>();

            this.taskManager = module.getCore().getTaskManager();
            this.taskID = -1;
        }

        public void add(TNTPrimed tnt)
        {
            this.noBlockDamageSet.add(tnt);
        }

        public void removeDeadTNT()
        {
            Iterator<TNTPrimed> tntIterator = this.noBlockDamageSet.iterator();
            while(tntIterator.hasNext())
            {
                TNTPrimed tnt = tntIterator.next();
                if(tnt.isDead())
                {
                    tntIterator.remove();
                }
            }
        }

        public boolean contains(TNTPrimed tnt)
        {
            return this.noBlockDamageSet.contains(tnt);
        }

        @Listener
        public void onEntityExplode(final ExplosionEvent event)
        {
            try
            {
                if (event.getEntityType() == EntityType.PRIMED_TNT && this.contains((TNTPrimed)event.getEntity()))
                {
                    event.blockList().clear();

                    if(!this.taskManager.isQueued(this.taskID) && !this.taskManager.isCurrentlyRunning(this.taskID))
                    {
                        this.taskID = this.taskManager.runTaskDelayed(module, new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                removeDeadTNT();
                            }
                        }, 1);
                    }
                }
            }
            catch (NullPointerException ignored)
            {}
        }

        @Listener
        public void onEntityDamageByEntity(final EntityDamageByEntityEvent event)
        {
            if(event.getDamager() instanceof TNTPrimed && this.contains((TNTPrimed)event.getDamager()))
            {
                event.setCancelled(true);
            }
        }
    }
}
