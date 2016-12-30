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
package org.cubeengine.module.shout.interactions;

import java.io.IOException;
import java.util.Collection;
import java.util.Locale;

import de.cubeisland.engine.reflect.annotations.Comment;
import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.alias.Alias;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
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
    public void show(CommandSource context, String announcement)
    {
        Announcement toShow = this.module.getAnnouncementManager().getAnnouncement(announcement);
        if (toShow == null)
        {
            i18n.sendTranslated(context, NEGATIVE, "{input#announcement} was not found!", announcement);
            return;
        }
        toShow.announce();
        i18n.sendTranslated(context, POSITIVE, "The announcement {name} has been announced!", toShow.getName());
    }

    @Alias(value = {"announcements"})
    @Command(alias = "announcements", desc = "List all announcements")
    public void list(CommandSource context)
    {
        Collection<Announcement> list = this.module.getAnnouncementManager().getAllAnnouncements();
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
            this.module.getAnnouncementManager().addAnnouncement(
                this.module.getAnnouncementManager().createAnnouncement(
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

        module.getAnnouncementManager().reload();

        i18n.sendTranslated(ctx, POSITIVE, "Your announcement have been created and loaded into the plugin");
    }

    @Command(desc = "clean all loaded announcements from memory and load from disk")
    public void reload(CommandContext context)
    {
        module.getAnnouncementManager().reload();
        context.sendTranslated(POSITIVE, "All the announcements have now been reloaded, and the players have been re-added");
    }

    @Command(desc = "delete an announcement")
    public void delete(CommandContext context, String announcement)
    {
        if (module.getAnnouncementManager().deleteAnnouncement(announcement))
        {
            context.sendTranslated(POSITIVE, "Announcement {name} was deleted!", announcement);
        }
        else
        {
            context.sendTranslated(POSITIVE, "There is now announcement named {}", announcement);
        }
    }

    @Command(desc = "modifies an announcement")
    public void modify(CommandContext context, String announcement, String message, @Named("locale") Locale locale)
    {
        Announcement a = module.getAnnouncementManager().getAnnouncement(announcement);
        if (a == null)
        {
            context.sendTranslated(POSITIVE, "There is now announcement named {}", announcement);
            return;
        }
        if (locale == null)
        {
            a.getConfig().announcement = message;
        }
        else
        {
            a.getConfig().translated.put(locale, message);
        }
        a.getConfig().save();
        context.sendTranslated(POSITIVE, "Updated announcement");
    }
}
