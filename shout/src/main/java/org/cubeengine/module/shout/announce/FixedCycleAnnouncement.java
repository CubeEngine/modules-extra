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
package org.cubeengine.module.shout.announce;

import org.cubeengine.module.shout.Shout;
import org.cubeengine.module.shout.announce.task.FixedCycleTask;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.libcube.service.task.TaskManager;

public class FixedCycleAnnouncement extends Announcement
{
    private FixedCycleTask task;

    public FixedCycleAnnouncement(Shout module, String name, AnnouncementConfig config, PermissionManager pm, TaskManager tm)
    {
        super(module, name, config, pm);
        task = new FixedCycleTask(tm, this, module);
    }

    public void stop()
    {
        task.stop();
    }

    public FixedCycleAnnouncement start()
    {
        task.run();
        return this;
    }
}
