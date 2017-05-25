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
package org.cubeengine.module.hide;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.spongepowered.api.text.format.TextColors.YELLOW;

public class HideCommands
{
    private final Hide module;
    private I18n i18n;

    public HideCommands(Hide module, I18n i18n)
    {
        this.module = module;
        this.i18n = i18n;
    }

    @Command(desc = "Hides a player.")
    public void hide(CommandSource context, @Default Player player)
    {
        if (!this.module.isHidden(player))
        {
            this.module.hidePlayer(player);
            if (context.equals(player))
            {
                i18n.sendTranslated(player, POSITIVE, "You are now hidden!");
                return;
            }
            i18n.sendTranslated(player, POSITIVE, "You were hidden by {sender}!", context);
            i18n.sendTranslated(context, POSITIVE, "{user} is now hidden!", player);
            return;
        }
        if (context.equals(player))
        {
            i18n.sendTranslated(player, NEUTRAL, "You are already hidden!");
            return;
        }
        i18n.sendTranslated(context, NEUTRAL, "{user} is already hidden!", player);
    }

    @Command(desc = "Unhides a player.")
    public void unhide(CommandSource context, @Default Player player)
    {
        if (this.module.isHidden(player))
        {
            this.module.showPlayer(player);
            if (context.equals(player))
            {
                i18n.sendTranslated(player, POSITIVE, "You are now visible!");
                return;
            }
            i18n.sendTranslated(player, POSITIVE, "You were unhidden by {sender}!", context);
            i18n.sendTranslated(context, POSITIVE, "{user} is now visible!", player);
            return;
        }
        if (context.equals(player))
        {
            i18n.sendTranslated(player, NEUTRAL, "You are already visible!");
            return;
        }
        i18n.sendTranslated(context, NEUTRAL, "{user} is already visible!", player);
    }

    @Command(desc = "Checks whether a player is hidden.")
    public void hidden(CommandSource context, @Default Player player)
    {
        if (this.module.isHidden(player))
        {
            if (context.equals(player))
            {
                i18n.sendTranslated(context, POSITIVE, "You are currently hidden!");
                return;
            }
            i18n.sendTranslated(context, POSITIVE, "{user} is currently hidden!", player);
            return;
        }
        if (context.equals(player))
        {
            i18n.sendTranslated(context, NEUTRAL, "You are currently visible!");
            return;
        }
        i18n.sendTranslated(context, NEUTRAL, "{user} is currently visible!", player);
    }

    @Command(desc = "Lists all hidden players.")
    public void listhiddens(CommandSource context) throws ExecutionException, InterruptedException
    {
        Set<UUID> hiddens = this.module.getHiddenUsers();
        if (hiddens.isEmpty())
        {
            i18n.sendTranslated(context, NEUTRAL, "There are no hidden users!");
            return;
        }
        i18n.sendTranslated(context, POSITIVE, "The following users are hidden:");
        for (UUID uuid : hiddens)
        {
            context.sendMessage(Text.of(" - ", YELLOW, Sponge.getServer().getGameProfileManager().get(uuid).get().getName()));
        }
    }

    /*
    @Command(desc = "Toggles the ability to see hidden players.")
    public void seehiddens(CommandSource context, @Default Player player)
    {
        if (this.module.toggleCanSeeHiddens(player))
        {
            if (context.equals(player))
            {
                i18n.sendTranslated(context, POSITIVE, "You can now see hidden users!");
                return;
            }
            i18n.sendTranslated(player, POSITIVE, "You can now see hidden users! (Enabled by {sender})", context);
            i18n.sendTranslated(context, NEUTRAL, "{user} can now see hidden users!", player);
            return;
        }
        if (context.equals(player))
        {
            i18n.sendTranslated(context, POSITIVE, "You can no longer see hidden users!");
            return;
        }
        i18n.sendTranslated(player, POSITIVE, "You can no longer see hidden users! (Disabled by {sender})", context);
        i18n.sendTranslated(context, NEUTRAL, "{user} can no longer see hidden users!", player);
    }

    @Command(desc = "Checks whether a player can see hidden players.")
    public void canseehiddens(CommandSource context, @Default Player player)
    {
        if (this.module.canSeeHiddens(player))
        {
            if (context.equals(player))
            {
                i18n.sendTranslated(context, POSITIVE, "You can currently see hidden users!");
                return;
            }
            i18n.sendTranslated(context, POSITIVE, "{user} can currently see hidden users!", player);
            return;
        }
        if (context.equals(player))
        {
            i18n.sendTranslated(context, NEUTRAL, "You can't see hidden players!");
            return;
        }
        i18n.sendTranslated(context, NEUTRAL, "{user} can't see hidden players!", player);
    }

    @Command(desc = "Lists all players who can see hidden players.")
    public void listcanseehiddens(CommandSource context)
    {
        Set<UUID> canSeeHiddens = this.module.getCanSeeHiddens();
        if (canSeeHiddens.isEmpty())
        {
            i18n.sendTranslated(context, NEUTRAL, "No users can currently see hidden users!");
            return;
        }
        i18n.sendTranslated(context, POSITIVE, "The following players can see hidden players:");
        for (UUID canSee : canSeeHiddens)
        {
            context.sendMessage(" - " + YELLOW + module.getCore().getUserManager().getExactUser(canSee).getDisplayName());
        }
    }
    */

}
