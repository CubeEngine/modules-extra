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

import org.cubeengine.service.permission.PermissionContainer;
import org.spongepowered.api.service.permission.PermissionDescription;

@SuppressWarnings("all")
public class ChatPerm extends PermissionContainer<Chat>
{
    public ChatPerm(Chat module)
    {
        super(module);
    }

    private final PermissionDescription COMMAND = register("command", "Base Commands Permission", null);

    public final PermissionDescription COLOR = register("color", "Allows using color codes in chat", null);

    public final PermissionDescription COMMAND_NICK_OTHER = register("nick.other", "", COMMAND);
    public final PermissionDescription COMMAND_NICK_OFOTHER = register("nick.of-other", "Allows to set the nickname to a players name that plays on this server", COMMAND);

    public final PermissionDescription PREVENT_AUTOUNAFK = register("afk.prevent.autounafk", "Prevents from being displayed as no longer afk automatically unless using chat", COMMAND);
    public final PermissionDescription PREVENT_AUTOAFK = register("afk.prevent.autoafk", "Prevents from being displayed as afk automatically", COMMAND);
    public final PermissionDescription COMMAND_AFK_OTHER = register("afk.other", "Allows to set or unset the afk status of other players", COMMAND);

    public final PermissionDescription COMMAND_IGNORE_PREVENT = register("ignore.prevent", "Prevents adding the player with this permission to an ignore-list", COMMAND);
}
