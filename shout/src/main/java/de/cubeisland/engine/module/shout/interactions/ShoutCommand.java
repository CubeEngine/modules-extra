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
package de.cubeisland.engine.module.shout.interactions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import javax.xml.ws.RequestWrapper;
import de.cubeisland.engine.command.alias.Alias;
import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.Flag;
import de.cubeisland.engine.command.methodic.Flags;
import de.cubeisland.engine.command.methodic.Param;
import de.cubeisland.engine.command.methodic.Params;
import de.cubeisland.engine.command.methodic.parametric.Label;
import de.cubeisland.engine.command.methodic.parametric.Named;
import de.cubeisland.engine.command.result.CommandResult;
import de.cubeisland.engine.core.command.CommandContainer;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.command.CommandSender;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.ChatFormat;
import de.cubeisland.engine.i18n.I18nUtil;
import de.cubeisland.engine.module.shout.Shout;
import de.cubeisland.engine.module.shout.announce.Announcement;
import de.cubeisland.engine.module.shout.announce.MessageOfTheDay;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import static de.cubeisland.engine.core.util.formatter.MessageType.*;

@Command(name = "shout", desc = "Announce a message to players on the server", alias = "announce")
public class ShoutCommand extends CommandContainer
{
    private final Shout module;

    public ShoutCommand(Shout module)
    {
        super(module);
        this.module = module;
    }

    public CommandResult run(CommandContext context)
    {
        Announcement announcement = this.module.getAnnouncementManager().getAnnouncement(context.getString(0));
        if (announcement == null)
        {
            context.sendTranslated(NEGATIVE, "{input#announcement} was not found!", context.get(0));
            return null;
        }
        List<Player> players;

        if (announcement.getFirstWorld().equals("*"))
        {
            players = new ArrayList<>(Bukkit.getOnlinePlayers());
        }
        else
        {
            players = new ArrayList<>();
            for (String world : announcement.getWorlds())
            {
                World w = Bukkit.getWorld(world);
                if (w != null)
                {
                    players.addAll(Bukkit.getWorld(world).getPlayers());
                }
            }
        }

        for (Player player : players)
        {
            User u = this.module.getCore().getUserManager().getExactUser(player.getUniqueId());
            String[] message = announcement.getMessage(u.getLocale());
            if (message != null)
            {
                u.sendMessage("");
                for (String line : message)
                {
                    u.sendMessage(ChatFormat.parseFormats(line));
                }
                u.sendMessage("");
            }
        }
        context.sendTranslated(POSITIVE, "The announcement {name} has been announced!", announcement.getName());
        return null;
    }

    @Alias(value = {"announcements"})
    @Command(alias = "announcements", desc = "List all announcements")
    public void list(CommandContext context)
    {
        Iterator<Announcement> iter = this.module.getAnnouncementManager().getAllAnnouncements().iterator();
        if (iter.hasNext())
        {
            context.sendTranslated(POSITIVE, "Here is the list of announcements:");
            while (iter.hasNext())
            {
                context.sendMessage(" - " + iter.next().getName());
            }
        }
        else
        {
            context.sendTranslated(NEGATIVE, "There are no announcements loaded!");
        }
    }

    @Command(desc = "Creates a new announcement")
    public void create(CommandSender context, String name,
                       @Named({"message", "m"}) String message,
                       @Named({"delay", "d"}) @Label("<x> minutes|hours|days") String delay,
                       @Named({"world", "w"}) String world,
                       @Named({"permission", "p"}) String permission,
                       @Named({"group", "g"}) String group,
                       @Named({"locale", "l"}) String locale,
                       @Flag(name = "fc", longName = "fixed-cycle") boolean fixedCycle)
    {
        if (message == null)
        {
            context.sendTranslated(NEUTRAL, "You have to include a message!");
            return;
        }

        Locale aLocale = context.getLocale();
        if (locale != null)
        {
            aLocale = I18nUtil.stringToLocale(locale);
        }
        if (aLocale == null)
        {
            context.sendTranslated(NEGATIVE, "{input#locale} isn't a valid locale!", locale);
        }

        try
        {
            this.module.getAnnouncementManager().addAnnouncement(
                this.module.getAnnouncementManager().createAnnouncement(
                    name,
                    aLocale,
                    message,
                    delay == null ? "10 minutes" : delay,
                    world == null ? "*" : world,
                    permission == null ? "*" : permission,
                    fixedCycle));
        }
        catch (IllegalArgumentException ex)
        {
            context.sendTranslated(NEGATIVE, "Some of your arguments are not valid.");
            context.sendTranslated(NEGATIVE, "The error message was: {}", ex.getLocalizedMessage());
        }
        catch (IOException ex)
        {
            context.sendTranslated(NEGATIVE, "There was an error creating some of the files.");
            context.sendTranslated(NEGATIVE, "The error message was: {}", ex.getLocalizedMessage());
        }

        module.getAnnouncementManager().reload();

        context.sendTranslated(POSITIVE, "Your announcement have been created and loaded into the plugin");
    }

    @Command(desc = "clean all loaded announcements form memory and load from disk")
    public void reload(CommandContext context)
    {
        module.getAnnouncementManager().reload();
        context.sendTranslated(POSITIVE, "All the announcements have now been reloaded, and the players have been re-added");
    }

    @Alias(value = "motd")
    @Command(desc = "Prints out the message of the day.")
    public void motd(CommandContext context)
    {
        MessageOfTheDay motd = this.module.getAnnouncementManager().getMotd();
        if (motd != null)
        {
            context.sendMessage(" ");
            for (String line : motd.getMessage(context.getSource().getLocale()))
            {
                context.sendMessage(line);
            }
        }
        else
        {
            context.sendTranslated(NEUTRAL, "There is no message of the day.");
        }
    }
}
