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

import java.util.HashMap;
import java.util.UUID;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.module.fun.Fun;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.task.TaskManager;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.world.World;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

public class DiscoCommand
{
    private final Fun module;
    private final HashMap<UUID, DiscoTask> activeTasks;
    private I18n i18n;
    private TaskManager tm;

    public DiscoCommand(Fun module, I18n i18n, TaskManager tm)
    {
        this.module = module;
        this.i18n = i18n;
        this.tm = tm;
        this.activeTasks = new HashMap<>();
    }

    @Command(desc = "Rapidly changes from day to night")
    public void disco(CommandSource context, @Default World world, @Optional Integer delay)
    {
        delay = delay == null ? this.module.getConfig().command.disco.defaultDelay : delay;
        if (delay < this.module.getConfig().command.disco.minDelay || delay > this.module.getConfig().command.disco.maxDelay)
        {
            i18n.sendTranslated(context, NEGATIVE, "The delay has to be a number between {integer} and {integer}", this.module.getConfig().command.disco.minDelay, this.module.getConfig().command.disco.maxDelay);
            return;
        }

        DiscoTask runningTask = activeTasks.remove(world.getUniqueId());
        if (runningTask != null)
        {
            runningTask.stop();
            i18n.sendTranslated(context, POSITIVE, "The disco has been stopped!");
        }
        else
        {
            activeTasks.put(world.getUniqueId(), new DiscoTask(world, delay, tm));
            i18n.sendTranslated(context, POSITIVE, "The disco started!");

        }
    }

    private class DiscoTask implements Runnable
    {
        private TaskManager tm;
        private final World world;
        private long originalTime;
        private UUID taskID;

        public DiscoTask(World world, final long delay, TaskManager tm)
        {
            this.world = world;
            this.tm = tm;
            this.originalTime = world.getProperties().getTotalTime();
            this.taskID = tm.runTimer(Fun.class, this, delay, delay);
        }

        public void stop()
        {
            tm.cancelTask(Fun.class, taskID);
            world.getProperties().setWorldTime(originalTime);
        }

        @Override
        public void run()
        {
            if (this.world.getProperties().getWorldTime() > 12000)
            {
                this.world.getProperties().setWorldTime(6000);
            }
            else
            {
                this.world.getProperties().setWorldTime(18000);
            }
        }
    }
}
