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
import de.cubeisland.engine.modularity.core.marker.Disable;
import de.cubeisland.engine.modularity.core.marker.Enable;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import org.cubeengine.module.chat.command.AfkCommand;
import org.cubeengine.module.chat.command.ChatCommands;
import org.cubeengine.module.chat.command.IgnoreCommands;
import org.cubeengine.module.chat.command.MuteCommands;
import org.cubeengine.module.chat.listener.ChatFormatListener;
import org.cubeengine.module.chat.listener.MuteListener;
import org.cubeengine.module.chat.storage.TableIgnorelist;
import org.cubeengine.module.chat.storage.TableMuted;
import org.cubeengine.service.filesystem.FileManager;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.module.core.sponge.EventManager;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.database.Database;
import org.cubeengine.service.permission.PermissionManager;
import org.cubeengine.service.task.TaskManager;
import org.cubeengine.service.user.Broadcaster;
import org.cubeengine.service.user.UserManager;
import org.spongepowered.api.Game;


/**
 * /me 	Displays a message about yourself.
 * /tell (msg) Displays a private message to other players.
 */
@ModuleInfo(name = "Chat", description = "Chat formatting")
public class Chat extends Module
{
    // TODO tablist-prefix data from subject or other module?
    private ChatConfig config;
    private ChatPerm perms;

    @Inject private UserManager um;
    @Inject private FileManager fm;
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
        this.config = fm.loadConfig(this, ChatConfig.class);
        this.perms = new ChatPerm(this);
        db.registerTable(TableMuted.class);
        db.registerTable(TableIgnorelist.class);
        MuteCommands muteCmd = new MuteCommands(this, db, um, i18n);
        cm.addCommands(this, muteCmd);
        IgnoreCommands ignoreCmd = new IgnoreCommands(this, db, um);

        cm.addCommands(this, ignoreCmd);
        em.registerListener(this, new ChatFormatListener(this, game, i18n));
        em.registerListener(this, new MuteListener(ignoreCmd, muteCmd, i18n));

        AfkCommand afkCmd = new AfkCommand(this, config.autoAfk.after.getMillis(), config.autoAfk.check.getMillis(), um, bc, tm, em, game);
        cm.addCommands(this, afkCmd);
        cm.addCommands(this, new ChatCommands(this, game, um, cm, i18n, bc, afkCmd));
    }

    @Disable
    public void onDisable()
    {
        em.removeListeners(this);
        cm.removeCommands(this);
        pm.cleanup(this);
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
