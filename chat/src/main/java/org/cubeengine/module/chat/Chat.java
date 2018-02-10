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
package org.cubeengine.module.chat;

import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.InjectService;
import org.cubeengine.libcube.service.Broadcaster;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.event.ModuleListener;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.module.chat.command.AfkCommand;
import org.cubeengine.module.chat.command.ChatCommands;
import org.cubeengine.module.chat.listener.ChatFormatListener;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.service.permission.PermissionService;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * /me 	Displays a message about yourself.
 * /tell (msg) Displays a private message to other players.
 */
// TODO link item in hand
@Singleton
@Module
public class Chat extends CubeEngineModule
{
    // TODO tablist-prefix data from subject or other module?
    @ModuleConfig private ChatConfig config;
    @Inject private ChatPerm perms;

    @Inject private EventManager em;
    @Inject private CommandManager cm;
    @Inject private I18n i18n;
    @Inject private TaskManager tm;
    @Inject private Broadcaster bc;

    @ModuleListener private ChatFormatListener chatFormatListener;
    @InjectService private PermissionService ps;

    @Listener
    public void onEnable(GamePreInitializationEvent event)
    {
        AfkCommand afkCmd = new AfkCommand(this, config.autoAfk.after.toMillis(), config.autoAfk.check.toMillis(), bc, tm, em);
        cm.addCommands(this, afkCmd);
        cm.addCommands(this, new ChatCommands(this, i18n, bc, afkCmd));
    }

    public ChatConfig getConfig()
    {
        return config;
    }

    public ChatPerm perms()
    {
        return perms;
    }

    public PermissionService getPermissionService() {
        return ps;
    }

    public I18n getI18n() {
        return i18n;
    }
}
