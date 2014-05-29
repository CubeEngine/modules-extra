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

import de.cubeisland.engine.core.command.CubeContext;
import de.cubeisland.engine.core.command.reflected.Command;
import de.cubeisland.engine.core.command.reflected.context.Grouped;
import de.cubeisland.engine.core.command.reflected.context.IParams;
import de.cubeisland.engine.core.command.reflected.context.Indexed;
import de.cubeisland.engine.core.user.User;

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
    @IParams(@Grouped(value = @Indexed(label = "message"), greedy = true))
    public void me(CubeContext context)
    {
        String message = context.getStrings(0);
        this.module.getCore().getUserManager().broadcastStatus(message, context.getSender());
    }

    @Command(desc = "Changes your display name")
    @IParams({@Grouped(@Indexed(label = {"name","!reset"})),
              @Grouped(req = false, value = @Indexed(label = "player", type = User.class))})
    public void nick(CubeContext context)
    {
        User forUser;
        if (context.hasIndexed(1))
        {
            forUser = context.getArg(1);
            if (forUser == null)
            {
                context.sendTranslated(NEGATIVE, "Player {user} not found!", context.getArg(1));
                return;
           }
           if (forUser != context.getSender() && !module.perms().COMMAND_NICK_OTHER.isAuthorized(context.getSender()))
           {
               context.sendTranslated(NEGATIVE, "You are not allowed to change the nickname of another player!");
               return;
           }
        }
        else if (context.getSender() instanceof User)
        {
            forUser = (User)context.getSender();
        }
        else
        {
            context.sendTranslated(NEGATIVE, "You cannot change the consoles display name");
            return;
        }
        String name = context.getArg(0);
        if (name.equalsIgnoreCase("-r") || name.equalsIgnoreCase("-reset"))
        {
            forUser.setDisplayName(context.getSender().getName());
            context.sendTranslated(POSITIVE, "Display name reset to {user}", context.getSender());
        }
        else
        {
            if (module.getCore().getUserManager().findExactUser(name) != null && !module.perms().COMMAND_NICK_OFOTHER.isAuthorized(context.getSender()))
            {
                context.sendTranslated(NEGATIVE, "This name has been taken by another player!");
                return;
            }
            context.sendTranslated(POSITIVE, "Display name changed from {user} to {user}", context.getSender(), name);
            ((User)context.getSender()).setDisplayName(name);
        }
    }
}
