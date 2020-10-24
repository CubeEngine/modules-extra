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

import com.google.inject.Singleton;
import org.cubeengine.libcube.InjectService;
import org.cubeengine.libcube.service.command.annotation.ModuleCommand;
import org.cubeengine.libcube.service.event.ModuleListener;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.module.chat.command.AfkCommand;
import org.cubeengine.module.chat.command.ChatCommands;
import org.cubeengine.module.chat.listener.ChatFormatListener;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Server;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.service.permission.PermissionService;

/**
 * /me 	Displays a message about yourself.
 * /tell (msg) Displays a private message to other players.
 */
// TODO link item in hand
@Singleton
@Module
public class Chat
{
    // TODO tablist-prefix data from subject or other module?
    @ModuleConfig private ChatConfig config;

    @ModuleListener private ChatFormatListener chatFormatListener;

    @ModuleCommand private AfkCommand afkCommand;
    @ModuleCommand private ChatCommands chatCommands;

    @InjectService private PermissionService ps;

    @Listener
    public void onStarted(StartedEngineEvent<Server> event)
    {
        this.afkCommand.init(config);
        this.chatFormatListener.init(config);
    }

    public PermissionService getPermissionService()
    {
        return ps;
    }
}
