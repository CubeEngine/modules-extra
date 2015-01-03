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

import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.Param;
import de.cubeisland.engine.command.methodic.Params;
import de.cubeisland.engine.command.methodic.parametric.Greed;
import de.cubeisland.engine.command.methodic.parametric.Label;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.user.User;

import static de.cubeisland.engine.command.parameter.Parameter.INFINITE;
import static de.cubeisland.engine.command.parameter.property.Requirement.OPTIONAL;
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
    public void me(CommandContext context, @Label("message") @Greed(INFINITE) String message)
    {
        this.module.getCore().getUserManager().broadcastStatus(message, context.getSource());
    }

    @Command(desc = "Changes your display name")
    @Params(positional = {@Param(label = "name"), // TODO static values , staticValues = {"-r", "-reset"}
                          @Param(req = OPTIONAL, label = "player", type = User.class)})
    public void nick(CommandContext context)
    {
        User forUser;
        if (context.hasPositional(1))
        {
            forUser = context.get(1);
            if (forUser == null)
            {
                context.sendTranslated(NEGATIVE, "Player {user} not found!", context.get(1));
                return;
           }
           if (forUser != context.getSource() && !module.perms().COMMAND_NICK_OTHER.isAuthorized(context.getSource()))
           {
               context.sendTranslated(NEGATIVE, "You are not allowed to change the nickname of another player!");
               return;
           }
        }
        else if (context.getSource() instanceof User)
        {
            forUser = (User)context.getSource();
        }
        else
        {
            context.sendTranslated(NEGATIVE, "You cannot change the consoles display name"); // TODO You cannot?!?
            return;
        }
        String name = context.get(0);
        if (name.equalsIgnoreCase("-r") || name.equalsIgnoreCase("-reset"))
        {
            forUser.setDisplayName(context.getSource().getName());
            context.sendTranslated(POSITIVE, "Display name reset to {user}", context.getSource());
        }
        else
        {
            if (module.getCore().getUserManager().findExactUser(name) != null && !module.perms().COMMAND_NICK_OFOTHER.isAuthorized(context.getSource()))
            {
                context.sendTranslated(NEGATIVE, "This name has been taken by another player!");
                return;
            }
            context.sendTranslated(POSITIVE, "Display name changed from {user} to {user}", context.getSource(), name);
            ((User)context.getSource()).setDisplayName(name);
        }
    }
}
