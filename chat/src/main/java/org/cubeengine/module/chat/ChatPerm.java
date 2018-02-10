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

import javax.inject.Inject;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionContainer;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.spongepowered.api.service.permission.PermissionDescription;

@SuppressWarnings("all")
public class ChatPerm extends PermissionContainer
{

    @Inject
    public ChatPerm(PermissionManager pm)
    {
        super(pm, Chat.class);
    }


    public final Permission COLOR = register("color", "Allows using color codes in chat", null);

    public final Permission LONGER = register("longer", "Allows using a + at the end of chat to write longer messages", null);
    public final Permission NEWLINE = register("newline", "Allows using newlines (\\n) in chat", null);
    public final Permission COMMAND_NICK_OTHER = register("command.nick.other", "", null);
    public final Permission COMMAND_NICK_OFOTHER = register("command.nick.of-other", "Allows to set the nickname to a players name that plays on this server", null);

    public final Permission PREVENT_AUTOUNAFK = register("command.afk.prevent.autounafk", "Prevents from being displayed as no longer afk automatically unless using chat", null);
    public final Permission PREVENT_AUTOAFK = register("command.afk.prevent.autoafk", "Prevents from being displayed as afk automatically", null);
    public final Permission COMMAND_AFK_OTHER = register("command.afk.other", "Allows to set or unset the afk status of other players", null);
}
