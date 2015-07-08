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
package de.cubeisland.engine.module.chat.command;

import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Default;
import de.cubeisland.engine.module.chat.Chat;
import de.cubeisland.engine.module.chat.ChatAttachment;
import de.cubeisland.engine.module.chat.listener.AfkListener;
import de.cubeisland.engine.service.command.CommandContext;
import de.cubeisland.engine.service.user.User;
import de.cubeisland.engine.service.user.UserManager;

import static de.cubeisland.engine.service.i18n.formatter.MessageType.NEGATIVE;

public class AfkCommand
{
    private final Chat module;
    private final AfkListener listener;
    private final UserManager um;

    public AfkCommand(Chat module, AfkListener listener, UserManager um)
    {
        this.module = module;
        this.listener = listener;
        this.um = um;
    }

    @Command(desc = "Displays that you are afk")
    public void afk(CommandContext context, @Default User player)
    {
        if (!context.getSource().equals(player))
        {
            context.ensurePermission(module.perms().COMMAND_AFK_OTHER);
        }
        if (!player.getPlayer().isPresent())
        {
            context.sendTranslated(NEGATIVE, "{user} is not online!", player);
            return;
        }
        if (player.get(ChatAttachment.class).isAfk())
        {
            player.get(ChatAttachment.class).updateLastAction();
            listener.run();
            return;
        }
        player.get(ChatAttachment.class).setAfk(true);
        player.get(ChatAttachment.class).resetLastAction();
        um.broadcastStatus("is now afk.", player);
    }
}
