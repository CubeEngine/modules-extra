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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Flag;
import de.cubeisland.engine.butler.parametric.Named;
import de.cubeisland.engine.butler.parametric.Optional;
import de.cubeisland.engine.service.command.CommandSender;
import de.cubeisland.engine.service.task.TaskManager;
import de.cubeisland.engine.service.user.User;
import de.cubeisland.engine.module.core.util.math.Vector3;
import de.cubeisland.engine.module.core.util.math.shape.Cuboid;
import de.cubeisland.engine.module.core.util.math.shape.Cylinder;
import de.cubeisland.engine.module.core.util.math.shape.Shape;
import de.cubeisland.engine.module.core.util.math.shape.Sphere;
import de.cubeisland.engine.module.fun.Fun;
import org.spongepowered.api.world.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.util.Vector;

import de.cubeisland.engine.module.core.util.formatter.MessageType.NEGATIVE;
import static de.cubeisland.engine.module.core.util.formatter.MessageType.POSITIVE;

public class NukeCommand
{
    private final Fun module;
    private final NukeListener nukeListener;

    public NukeCommand(Fun module)
    {
        this.module = module;
        this.nukeListener = new NukeListener();

        module.getCore().getEventManager().registerListener(module, this.nukeListener);
    }

    @Command(desc = "Makes a carpet of TNT fall on a player or where you're looking")
    public void nuke(CommandSender context,
                     @Optional Integer param1,
                     @Optional Integer param2,
                     @Optional Integer param3,
                     @Named({"player", "p"}) User player,
                     @Named({"height", "h"}) Integer height,
                     @Named({"range", "r"}) Integer range,
                     @Named({"shape", "s"}) String shape,
                     @Flag boolean unsafe,
                     @Flag boolean quiet)
    {
        Location location;
        range = range == null ? 4 : range;
        height = height == null ? 5 : height;

        if(range != 4 && !module.perms().COMMAND_NUKE_CHANGE_RANGE.isAuthorized(context))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to change the explosion range of the nuke carpet!");
            return;
        }
        if(range < 0 || range > this.module.getConfig().command.nuke.maxExplosionRange)
        {
            context.sendTranslated(NEGATIVE, "The explosion range can't be less than 0 or greater than {integer}", this.module.getConfig().command.nuke.maxExplosionRange);
            return;
        }

        if(player != null)
        {
            if (!context.equals(player) && !module.perms().COMMAND_NUKE_OTHER.isAuthorized(context))
            {
                context.sendTranslated(NEGATIVE, "You are not allowed to specify a player!");
                return;
            }
            location = ((User)context).getLocation();
        }
        else
        {
            if(!(context instanceof User))
            {
                context.sendTranslated(NEGATIVE, "This command can only be used by a player!");
                return;
            }
            location = ((User)context).getTargetBlock(Collections.<Material>emptySet(), this.module.getConfig().command.nuke.distance).getLocation();
        }

        Shape aShape = this.getShape(context, shape, location, height, param1, param2, param3);
        if(aShape == null)
        {
            return;
        }

        int blockAmount = this.spawnNuke(aShape, location.getWorld(), range, unsafe);

        if(!quiet)
        {
            context.sendTranslated(POSITIVE, "You spawned {integer} blocks of tnt.", blockAmount);
        }
    }

    private Shape getShape(CommandSender context, String shape, Location location, int locationHeight, Integer param1,
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

            location = location.subtract(width / 2d, 0, depth / 2d);
            location = this.getSpawnLocation(location, locationHeight);
            return new Cuboid(new Vector3(location.getX(), location.getY(), location.getZ()), width, height, depth);
        case "sphere":
            int radius = param1 == null ? 1 : param1;
            location = this.getSpawnLocation(location, locationHeight);
            return new Sphere(new Vector3(location.getX(), location.getY(), location.getZ()), radius);
        default:
            context.sendTranslated(NEGATIVE, "The shape {input} was not found!", shape);
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
            if (location.getBlock().getType() == Material.AIR)
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

    private class NukeListener implements Listener
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

        @EventHandler
        public void onEntityExplode(final EntityExplodeEvent event)
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

        @EventHandler
        public void onEntityDamageByEntity(final EntityDamageByEntityEvent event)
        {
            if(event.getDamager() instanceof TNTPrimed && this.contains((TNTPrimed)event.getDamager()))
            {
                event.setCancelled(true);
            }
        }
    }
}
