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
package org.cubeengine.module.shout;

import java.nio.file.Path;
import javax.inject.Inject;
import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Disable;
import de.cubeisland.engine.modularity.core.marker.Enable;
import de.cubeisland.engine.reflect.Reflector;
import org.cubeengine.module.shout.announce.AnnouncementManager;
import org.cubeengine.module.shout.interactions.ShoutCommand;
import org.cubeengine.module.shout.interactions.ShoutListener;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.matcher.StringMatcher;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.libcube.service.task.TaskManager;
import org.spongepowered.api.service.permission.PermissionDescription;

@ModuleInfo(name = "Shout", description = "Announce things!")
public class Shout extends Module
{
    @Inject private PermissionManager pm;
    @Inject private Path modulePath;
    @Inject private Log log;
    @Inject private EventManager em;
    @Inject private CommandManager cm;
    @Inject private I18n i18n;
    @Inject private TaskManager tm;
    @Inject private StringMatcher sm;
    @Inject private Reflector reflector;

    private AnnouncementManager announcementManager;
    private PermissionDescription announcePerm;


    public PermissionDescription getAnnouncePerm()
    {
        return announcePerm;
    }

    @Enable
    public void onEnable()
    {
        announcePerm = pm.register(this, "announcement", "", null);

        announcementManager = new AnnouncementManager(this, modulePath, i18n, pm, tm, sm, reflector);
        announcementManager.loadAnnouncements();
        em.registerListener(this, new ShoutListener(announcementManager));
        cm.addCommand(new ShoutCommand(this, i18n));

        announcementManager.initUsers();
    }

    @Disable
    public void onDisable()
    {
    }

    public AnnouncementManager getAnnouncementManager()
    {
        return this.announcementManager;
    }

    public Log getLog()
    {
        return log;
    }
}
