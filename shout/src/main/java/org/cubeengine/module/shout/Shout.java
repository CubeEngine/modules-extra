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
package org.cubeengine.module.shout;

import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.matcher.StringMatcher;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.logscribe.Log;
import org.cubeengine.module.shout.announce.Announcement;
import org.cubeengine.module.shout.announce.AnnouncementManager;
import org.cubeengine.module.shout.interactions.ShoutCommand;
import org.cubeengine.module.shout.interactions.ShoutListener;
import org.cubeengine.processor.Module;
import org.cubeengine.reflect.Reflector;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;

import java.nio.file.Path;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Module
public class Shout extends CubeEngineModule
{
    @Inject private PermissionManager pm;
    private Path modulePath;
    private Log log;
    @Inject private EventManager em;
    @Inject private CommandManager cm;
    @Inject private I18n i18n;
    @Inject private TaskManager tm;
    @Inject private StringMatcher sm;
    @Inject private Reflector reflector;
    @Inject private ModuleManager mm;

    private AnnouncementManager manager;
    private Permission announcePerm;


    public Permission getAnnouncePerm()
    {
        return announcePerm;
    }

    @Listener
    public void onEnable(GamePreInitializationEvent event)
    {
        this.log = mm.getLoggerFor(Shout.class);
        this.modulePath = mm.getPathFor(Shout.class);
        announcePerm = pm.register(Shout.class, "announcement", "", null);

        manager = new AnnouncementManager(this, modulePath, i18n, pm, tm, sm, reflector);
        manager.loadAnnouncements();
        cm.getProviders().register(this, new AnnouncementParser(manager, i18n), Announcement.class);
        em.registerListener(Shout.class, new ShoutListener(manager));
        cm.addCommand(new ShoutCommand(cm, this, i18n));

    }

    public AnnouncementManager getManager()
    {
        return this.manager;
    }

    public Log getLog()
    {
        return log;
    }
}
