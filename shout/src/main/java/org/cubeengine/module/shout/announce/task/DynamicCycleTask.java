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

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import org.cubeengine.module.shout.Shout;
import org.cubeengine.module.shout.announce.Announcement;
import org.cubeengine.module.shout.announce.AnnouncementManager;
import org.cubeengine.libcube.service.task.TaskManager;
import org.spongepowered.api.entity.living.player.Player;

public class DynamicCycleTask implements Runnable
{
    private final TaskManager tm;
    private Player src;
    private Shout module;
    private AnnouncementManager manager;
    private Queue<Announcement> announcements = new LinkedList<>();
    private Announcement next;
    private UUID task;

    public DynamicCycleTask(TaskManager tm, Player src, Shout module, AnnouncementManager manager)
    {
        this.tm = tm;
        this.src = src;
        this.module = module;
        this.manager = manager;
    }

    public void addAnnouncement(Announcement announcement)
    {
        announcements.offer(announcement);
    }

    @Override
    public void run()
    {
        if (!src.isOnline())
        {
            manager.stop(src);
        }
        if (announcements.isEmpty())
        {
            delayNext();
            return;
        }
        if (next != null)
        {
            next.announce(src);
        }
        runNext();
    }

    private void delayNext()
    {
        if (task != null)
        {
            tm.cancelTask(module, task);
        }
        task = tm.runTaskDelayed(module, this, 600); // 30s
    }

    private void runNext()
    {
        int tries = announcements.size();
        do
        {
            next = announcements.poll();
            announcements.offer(next);

            if (next.canAccess(src))
            {
                if (task != null)
                {
                    tm.cancelTask(module, task);
                }
                task = tm.runTaskDelayed(module, this, next.getDelay());
                return;
            }
        }
        while (tries > 0);
        delayNext();
    }

    public void stop()
    {
        tm.cancelTask(module, task);
        task = null;
        next = null;
    }
}
