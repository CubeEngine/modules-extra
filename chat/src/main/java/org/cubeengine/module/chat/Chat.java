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

import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
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

import javax.inject.Inject;

/**
 * /me 	Displays a message about yourself.
 * /tell (msg) Displays a private message to other players.
 */
// TODO link item in hand
@ModuleInfo(name = "Chat", description = "Chat formatting")
public class Chat extends Module
{
    // TODO tablist-prefix data from subject or other module?
    @ModuleConfig private ChatConfig config;
    @Inject private ChatPerm perms;

    @Inject private EventManager em;
    @Inject private CommandManager cm;
    @Inject private I18n i18n;
    @Inject private TaskManager tm;
    @Inject private Broadcaster bc;

    @Inject @ModuleListener private ChatFormatListener chatFormatListener;

    @Enable
    public void onEnable()
    {
        AfkCommand afkCmd = new AfkCommand(this, config.autoAfk.after.getMillis(), config.autoAfk.check.getMillis(), bc, tm, em);
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
}
