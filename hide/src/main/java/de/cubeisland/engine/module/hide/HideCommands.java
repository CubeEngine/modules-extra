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
package de.cubeisland.engine.module.hide;

import java.util.Set;
import java.util.UUID;
import de.cubeisland.engine.command.parametric.Command;
import de.cubeisland.engine.command.parametric.Default;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.command.CommandSender;
import de.cubeisland.engine.core.user.User;

import static de.cubeisland.engine.core.util.ChatFormat.YELLOW;
import static de.cubeisland.engine.core.util.formatter.MessageType.*;

public class HideCommands
{
    private final Hide module;

    public HideCommands(Hide module)
    {
        this.module = module;
    }

    @Command(desc = "Hides a player.")
    public void hide(CommandSender context, @Default User player)
    {
        if (!this.module.isHidden(player))
        {
            this.module.hidePlayer(player);
            if (context.equals(player))
            {
                player.sendTranslated(POSITIVE, "You are now hidden!");
                return;
            }
            player.sendTranslated(POSITIVE, "You were hidden by {sender}!", context);
            context.sendTranslated(POSITIVE, "{user} is now hidden!", player);
            return;
        }
        if (context.equals(player))
        {
            player.sendTranslated(NEUTRAL, "You are already hidden!");
            return;
        }
        context.sendTranslated(NEUTRAL, "{user} is already hidden!", player);
    }

    @Command(desc = "Unhides a player.")
    public void unhide(CommandSender context, @Default User player)
    {
        if (this.module.isHidden(player))
        {
            this.module.showPlayer(player);
            if (context.equals(player))
            {
                player.sendTranslated(POSITIVE, "You are now visible!");
                return;
            }
            player.sendTranslated(POSITIVE, "You were unhidden by {sender}!", context);
            context.sendTranslated(POSITIVE, "{user} is now visible!", player);
            return;
        }
        if (context.equals(player))
        {
            player.sendTranslated(NEUTRAL, "You are already visible!");
            return;
        }
        context.sendTranslated(NEUTRAL, "{user} is already visible!", player);
    }

    @Command(desc = "Checks whether a player is hidden.")
    public void hidden(CommandSender context, @Default User player)
    {
        if (this.module.isHidden(player))
        {
            if (context.equals(player))
            {
                context.sendTranslated(POSITIVE, "You are currently hidden!");
                return;
            }
            context.sendTranslated(POSITIVE, "{user} is currently hidden!", player.getDisplayName());
            return;
        }
        if (context.equals(player))
        {
            context.sendTranslated(NEUTRAL, "You are currently visible!");
            return;
        }
        context.sendTranslated(NEUTRAL, "{user} is currently visible!", player.getDisplayName());
    }

    @Command(desc = "Lists all hidden players.")
    public void listhiddens(CommandContext context)
    {
        Set<UUID> hiddens = this.module.getHiddenUsers();
        if (hiddens.isEmpty())
        {
            context.sendTranslated(NEUTRAL, "There are no hidden users!");
            return;
        }
        context.sendTranslated(POSITIVE, "The following users are hidden:");
        for (UUID name : hiddens)
        {
            context.sendMessage(" - " + YELLOW + module.getCore().getUserManager().getExactUser(name).getDisplayName());
        }
    }

    @Command(desc = "Toggles the ability to see hidden players.")
    public void seehiddens(CommandSender context, @Default User player)
    {
        if (this.module.toggleCanSeeHiddens(player))
        {
            if (context.equals(player))
            {
                context.sendTranslated(POSITIVE, "You can now see hidden users!");
                return;
            }
            player.sendTranslated(POSITIVE, "You can now see hidden users! (Enabled by {sender})", context);
            context.sendTranslated(NEUTRAL, "{user} can now see hidden users!", player);
            return;
        }
        if (context.equals(player))
        {
            context.sendTranslated(POSITIVE, "You can no longer see hidden users!");
            return;
        }
        player.sendTranslated(POSITIVE, "You can no longer see hidden users! (Disabled by {sender})", context);
        context.sendTranslated(NEUTRAL, "{user} can no longer see hidden users!", player);
    }

    @Command(desc = "Checks whether a player can see hidden players.")
    public void canseehiddens(CommandSender context, @Default User player)
    {
        if (this.module.canSeeHiddens(player))
        {
            if (context.equals(player))
            {
                context.sendTranslated(POSITIVE, "You can currently see hidden users!");
                return;
            }
            context.sendTranslated(POSITIVE, "{user} can currently see hidden users!", player);
            return;
        }
        if (context.equals(player))
        {
            context.sendTranslated(NEUTRAL, "You can't see hidden players!");
            return;
        }
        context.sendTranslated(NEUTRAL, "{user} can't see hidden players!", player);
    }

    @Command(desc = "Lists all players who can see hidden players.")
    public void listcanseehiddens(CommandContext context)
    {
        Set<UUID> canSeeHiddens = this.module.getCanSeeHiddens();
        if (canSeeHiddens.isEmpty())
        {
            context.sendTranslated(NEUTRAL, "No users can currently see hidden users!");
            return;
        }
        context.sendTranslated(POSITIVE, "The following players can see hidden players:");
        for (UUID canSee : canSeeHiddens)
        {
            context.sendMessage(" - " + YELLOW + module.getCore().getUserManager().getExactUser(canSee).getDisplayName());
        }
    }
}
