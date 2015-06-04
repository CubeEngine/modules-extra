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
package de.cubeisland.engine.module.chat;

import javax.inject.Inject;
import de.cubeisland.engine.modularity.asm.marker.Disable;
import de.cubeisland.engine.modularity.asm.marker.Enable;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.module.core.filesystem.FileManager;
import de.cubeisland.engine.module.core.i18n.I18n;
import de.cubeisland.engine.module.core.sponge.EventManager;
import de.cubeisland.engine.module.service.command.CommandManager;
import de.cubeisland.engine.module.service.permission.PermissionManager;
import de.cubeisland.engine.module.service.user.UserManager;
import org.spongepowered.api.Game;

@ModuleInfo(name = "Chat", description = "Chat formatting")
public class Chat extends Module
{
    private ChatConfig config;
    private ChatPerm perms;

    @Inject private UserManager um;
    @Inject private FileManager fm;
    @Inject private EventManager em;
    @Inject private CommandManager cm;
    @Inject private PermissionManager pm;
    @Inject private I18n i18n;
    @Inject private Game game;

    @Enable
    public void onEnable()
    {
        this.config = fm.loadConfig(this, ChatConfig.class);
        this.perms = new ChatPerm(this);
        cm.addCommands(cm, this, new ChatCommands(this, um));
        em.registerListener(this, new ChatFormatListener(this, game, i18n));
    }

    @Disable
    public void onDisable()
    {
        em.removeListeners(this);
        cm.removeCommands(this);
        pm.removePermissions(this);
    }

    protected ChatConfig getConfig()
    {
        return config;
    }

    public ChatPerm perms()
    {
        return perms;
    }
}
