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

import de.cubeisland.engine.module.service.permission.Permission;
import de.cubeisland.engine.module.service.permission.PermissionContainer;

import static de.cubeisland.engine.module.service.permission.PermDefault.FALSE;

@SuppressWarnings("all")
public class ChatPerm extends PermissionContainer<Chat>
{
    public ChatPerm(Chat module)
    {
        super(module);
        this.registerAllPermissions();
    }

    public final Permission COMMAND = getBasePerm().childWildcard("command");

    public final Permission COLOR = getBasePerm().child("color");
    public final Permission COMMAND_NICK_OTHER = getBasePerm().childWildcard("command").childWildcard("nick").child("other");
    /**
     * Allows to set the nickname to a players name that plays on this server
     */
    public final Permission COMMAND_NICK_OFOTHER = getBasePerm().childWildcard("command").childWildcard("nick").child("of-other");

    private final Permission COMMAND_AFK = COMMAND.childWildcard("afk");
    private final Permission COMMAND_AFK_PREVENT = COMMAND_AFK.newWildcard("prevent");
    /**
     * Prevents from being displayed as no longer afk automatically unless using chat
     */
    public final Permission PREVENT_AUTOUNAFK = COMMAND_AFK_PREVENT.child("autounafk", FALSE);
    /**
     * Prevents from being displayed as afk automatically
     */
    public final Permission PREVENT_AUTOAFK = COMMAND_AFK_PREVENT.child("autoafk", FALSE);

    /**
     * Allows to set or unset the afk status of other players
     */
    public final Permission COMMAND_AFK_OTHER = COMMAND_AFK.child("other");

    public final Permission COMMAND_IGNORE_PREVENT = COMMAND.childWildcard("ignore").child("prevent", FALSE);
}
