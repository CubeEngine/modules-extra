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
package org.cubeengine.module.shout.interactions;

import java.io.IOException;
import java.util.Collection;
import java.util.Locale;

import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Flag;
import org.cubeengine.butler.parametric.Label;
import org.cubeengine.butler.parametric.Named;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.module.shout.Shout;
import org.cubeengine.module.shout.announce.Announcement;
import org.cubeengine.libcube.service.command.CommandContext;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

@Command(name = "shout", desc = "Announce a message to players on the server", alias = "announce")
public class ShoutCommand extends ContainerCommand
{
    private final Shout module;
    private I18n i18n;

    public ShoutCommand(CommandManager cm, Shout module, I18n i18n)
    {
        super(cm, Shout.class);
        this.module = module;
        this.i18n = i18n;
    }

    @Override
    protected boolean selfExecute(CommandInvocation invocation)
    {
        if (invocation.tokens().size() - invocation.consumed() == 1)
        {
            return getCommand("show").execute(invocation);
        }
        return super.selfExecute(invocation);
    }

    @Command(desc = "Displays an announcement")
    public void show(CommandSource context, Announcement announcement)
    {
        announcement.announce();
        i18n.sendTranslated(context, POSITIVE, "The announcement {name} has been announced!", announcement.getName());
    }

    @Alias(value = {"announcements"})
    @Command(alias = "announcements", desc = "List all announcements")
    public void list(CommandSource context)
    {
        Collection<Announcement> list = this.module.getManager().getAllAnnouncements();
        if (list.isEmpty())
        {
            i18n.sendTranslated(context, NEGATIVE, "There are no announcements loaded!");
            return;
        }
        i18n.sendTranslated(context, POSITIVE, "Here is the list of announcements:");
        for (Announcement announcement : list)
        {
            context.sendMessage(Text.of(" - ", announcement.getName()));
        }
    }

    @Command(desc = "Creates a new announcement")
    public void create(CommandSource ctx, String name,
                       @Named({"message", "m"}) String message,
                       @Named({"delay", "d"}) @Label("<x> minutes|hours|days") String delay,
                       @Named({"permission", "p"}) String permission,
                       @Named("weight") Integer weight,
                       @Flag(name = "fc", longName = "fixed-cycle") boolean fixedCycle)
    {
        weight = weight == null ? 1 : weight;
        if (message == null)
        {
            i18n.sendTranslated(ctx, NEUTRAL, "You have to include a message!");
            return;
        }

        try
        {
            this.module.getManager().addAnnouncement(
                this.module.getManager().createAnnouncement(
                    name, message,
                    delay == null ? "10 minutes" : delay,
                    permission == null ? "*" : permission,
                    fixedCycle, weight));
        }
        catch (IllegalArgumentException ex)
        {
            i18n.sendTranslated(ctx, NEGATIVE, "Some of your arguments are not valid.");
            i18n.sendTranslated(ctx, NEGATIVE, "The error message was: {}", ex.getLocalizedMessage());
        }
        catch (IOException ex)
        {
            i18n.sendTranslated(ctx, NEGATIVE, "There was an error creating some of the files.");
            i18n.sendTranslated(ctx, NEGATIVE, "The error message was: {}", ex.getLocalizedMessage());
        }

        module.getManager().reload();

        i18n.sendTranslated(ctx, POSITIVE, "Announcement {name} created.", name);
    }

    @Command(desc = "clean all loaded announcements from memory and load from disk")
    public void reload(CommandContext context)
    {
        module.getManager().reload();
        context.sendTranslated(POSITIVE, "All the announcements have now been reloaded, and the players have been re-added");
    }

    @Command(desc = "delete an announcement")
    public void delete(CommandContext context, String announcement)
    {
        if (module.getManager().deleteAnnouncement(announcement))
        {
            context.sendTranslated(POSITIVE, "Announcement {name} was deleted!", announcement);
        }
        else
        {
            context.sendTranslated(POSITIVE, "There is now announcement named {}", announcement);
        }
    }

    @Command(desc = "modifies an announcement")
    public void modify(CommandContext context, Announcement announcement, String message, @Named("locale") Locale locale, @Flag boolean append)
    {
        message = message.replace("\\n", "\n");
        if (locale == null)
        {
            announcement.getConfig().announcement = append ? announcement.getConfig().announcement + message : message;
        }
        else
        {
            if (append)
            {
                message = announcement.getConfig().translated.getOrDefault(locale, "") + message;
            }
            announcement.getConfig().translated.put(locale, message);
        }
        announcement.getConfig().save();
        context.sendTranslated(POSITIVE, "Updated announcement");
    }
}
