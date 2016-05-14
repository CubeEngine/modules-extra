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
package org.cubeengine.module.shout.announce.task;

import java.util.UUID;
import org.cubeengine.module.shout.Shout;
import org.cubeengine.module.shout.announce.FixedCycleAnnouncement;
import org.cubeengine.libcube.service.task.TaskManager;

public class FixedCycleTask implements Runnable
{
    private final TaskManager tm;
    private final FixedCycleAnnouncement announcement;
    private Shout module;
    private UUID task;

    public FixedCycleTask(TaskManager tm, FixedCycleAnnouncement announcement, Shout module)
    {
        this.tm = tm;
        this.announcement = announcement;
        this.module = module;
    }

    @Override
    public void run()
    {
        announcement.announce();
        if (task != null)
        {
            tm.cancelTask(Shout.class, task);
        }
        task = tm.runTaskDelayed(Shout.class, this, announcement.getDelay());
    }

    public void stop()
    {
        tm.cancelTask(Shout.class, task);
        task = null;
    }
}
