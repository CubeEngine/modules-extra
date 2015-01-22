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

import de.cubeisland.engine.command.parametric.Command;
import de.cubeisland.engine.command.parametric.Greed;
import de.cubeisland.engine.command.parametric.Label;
import de.cubeisland.engine.command.parametric.Optional;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.command.CommandSender;
import de.cubeisland.engine.core.user.User;

import static de.cubeisland.engine.command.parameter.Parameter.INFINITE;
import static de.cubeisland.engine.core.util.formatter.MessageType.NEGATIVE;
import static de.cubeisland.engine.core.util.formatter.MessageType.POSITIVE;

public class ChatCommands
{
    private final Chat module;

    public ChatCommands(Chat module)
    {
        this.module = module;
    }

    @Command(desc = "Allows you to emote")
    public void me(CommandSender context, @Greed(INFINITE) String message)
    {
        this.module.getCore().getUserManager().broadcastStatus(message, context);
    }

    @Command(desc = "Changes your display name")
    public void nick(CommandSender context, @Label("<name>|-reset") String name, @Optional User player)
    {
        if (player == null)
        {
            if (!(context instanceof User))
            {
                context.sendTranslated(NEGATIVE, "You cannot change the consoles display name"); // TODO You cannot?!?
                return;
            }
            player = (User)context;
        }

        if (!context.equals(player) && !module.perms().COMMAND_NICK_OTHER.isAuthorized(context))
        {
            context.sendTranslated(NEGATIVE, "You are not allowed to change the nickname of another player!");
            return;
        }

        if ("-r".equalsIgnoreCase(name) || "-reset".equalsIgnoreCase(name))
        {
            player.setDisplayName(context.getName());
            context.sendTranslated(POSITIVE, "Display name reset to {user}", context);
            return;
        }
        if (module.getCore().getUserManager().findExactUser(name) != null && !module.perms().COMMAND_NICK_OFOTHER.isAuthorized(context))
        {
            context.sendTranslated(NEGATIVE, "This name has been taken by another player!");
            return;
        }
        context.sendTranslated(POSITIVE, "Display name changed from {user} to {user}", context, name);
        ((User)context).setDisplayName(name);
    }
}
