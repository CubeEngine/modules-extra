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
package org.cubeengine.module.chat;

import javax.inject.Inject;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.cubeengine.module.chat.command.AfkCommand;
import org.cubeengine.module.chat.command.ChatCommands;
import org.cubeengine.module.chat.command.IgnoreCommands;
import org.cubeengine.module.chat.command.MuteCommands;
import org.cubeengine.module.chat.listener.ChatFormatListener;
import org.cubeengine.module.chat.listener.MuteListener;
import org.cubeengine.module.chat.storage.TableIgnorelist;
import org.cubeengine.module.chat.storage.TableMuted;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.database.Database;
import org.cubeengine.service.database.ModuleTables;
import org.cubeengine.service.event.EventManager;
import org.cubeengine.service.filesystem.ModuleConfig;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.permission.ModulePermissions;
import org.cubeengine.service.permission.PermissionManager;
import org.cubeengine.service.task.TaskManager;
import org.cubeengine.service.user.Broadcaster;
import org.spongepowered.api.Game;


/**
 * /me 	Displays a message about yourself.
 * /tell (msg) Displays a private message to other players.
 */
@ModuleInfo(name = "Chat", description = "Chat formatting")
@ModuleTables({TableMuted.class, TableIgnorelist.class})
public class Chat extends Module
{
    // TODO tablist-prefix data from subject or other module?
    @ModuleConfig private ChatConfig config;
    @ModulePermissions private ChatPerm perms;

    @Inject private EventManager em;
    @Inject private CommandManager cm;
    @Inject private PermissionManager pm;
    @Inject private I18n i18n;
    @Inject private Game game;
    @Inject private Database db;
    @Inject private TaskManager tm;
    @Inject private Broadcaster bc;

    @Enable
    public void onEnable()
    {
        MuteCommands muteCmd = new MuteCommands(this, db, i18n);
        cm.addCommands(this, muteCmd);
        IgnoreCommands ignoreCmd = new IgnoreCommands(this, db);

        cm.addCommands(this, ignoreCmd);
        em.registerListener(this, new ChatFormatListener(this, game, i18n));
        em.registerListener(this, new MuteListener(ignoreCmd, muteCmd, i18n));

        AfkCommand afkCmd = new AfkCommand(this, config.autoAfk.after.getMillis(), config.autoAfk.check.getMillis(), bc, tm, em, game);
        cm.addCommands(this, afkCmd);
        cm.addCommands(this, new ChatCommands(this, game, cm, i18n, bc, afkCmd));
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
