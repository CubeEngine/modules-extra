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
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Alias;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Delegate;
import org.cubeengine.libcube.service.command.annotation.Flag;
import org.cubeengine.libcube.service.command.annotation.Greedy;
import org.cubeengine.libcube.service.command.annotation.Label;
import org.cubeengine.libcube.service.command.annotation.Named;
import org.cubeengine.libcube.service.command.annotation.Using;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.shout.Shout;
import org.cubeengine.module.shout.announce.Announcement;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.parameter.managed.ValueParser;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

@Delegate("show")
@Using(ValueParser.class)
@Command(name = "shout", desc = "Announce a message to players on the server", alias = "announce")
public class ShoutCommand extends DispatcherCommand
{
    private final Shout module;
    private I18n i18n;

    public ShoutCommand(Shout module, I18n i18n)
    {
        this.module = module;
        this.i18n = i18n;
    }

    @Command(desc = "Displays an announcement")
    public void show(CommandCause context, Announcement announcement)
    {
        announcement.announce();
        i18n.send(context, POSITIVE, "The announcement {name} has been announced!", announcement.getName());
    }

    @Alias(value = "announcements")
    @Command(alias = "announcements", desc = "List all announcements")
    public void list(CommandCause context)
    {
        Collection<Announcement> list = this.module.getManager().getAllAnnouncements();
        if (list.isEmpty())
        {
            i18n.send(context, NEGATIVE, "There are no announcements loaded!");
            return;
        }
        i18n.send(context, POSITIVE, "Here is the list of announcements:");
        for (Announcement announcement : list)
        {
            context.sendMessage(Identity.nil(), Component.text(" - " + announcement.getName()));
        }
    }

    @Command(desc = "Creates a new announcement")
    public void create(CommandCause ctx, String name,
                       @Greedy String message,
                       @Named({"delay", "d"}) @Label("<x> minutes|hours|days") String delay,
                       @Named({"permission", "p"}) String permission,
                       @Named("weight") Integer weight,
                       @Flag(value = "fc", longName = "fixed-cycle") boolean fixedCycle,
                       @Flag boolean asJson)
    {
        weight = weight == null ? 1 : weight;
        if (message == null)
        {
            i18n.send(ctx, NEUTRAL, "You have to include a message!");
            return;
        }

        try
        {
            this.module.getManager().addAnnouncement(
                this.module.getManager().createAnnouncement(
                    name, asJson, message,
                    delay == null ? "10 minutes" : delay,
                    permission == null ? "*" : permission,
                    fixedCycle, weight));
        }
        catch (IllegalArgumentException ex)
        {
            i18n.send(ctx, NEGATIVE, "Some of your arguments are not valid.");
            i18n.send(ctx, NEGATIVE, "The error message was: {}", ex.getLocalizedMessage());
        }
        catch (IOException ex)
        {
            i18n.send(ctx, NEGATIVE, "There was an error creating some of the files.");
            i18n.send(ctx, NEGATIVE, "The error message was: {}", ex.getLocalizedMessage());
        }

        module.getManager().reload();

        i18n.send(ctx, POSITIVE, "Announcement {name} created.", name);
    }

    @Command(desc = "clean all loaded announcements from memory and load from disk")
    public void reload(CommandCause context)
    {
        module.getManager().reload();

        i18n.send(context, POSITIVE, "All the announcements have now been reloaded, and the players have been re-added");
    }

    @Command(desc = "delete an announcement")
    public void delete(CommandCause context, String announcement)
    {
        if (module.getManager().deleteAnnouncement(announcement))
        {
            i18n.send(context, POSITIVE, "Announcement {name} was deleted!", announcement);
        }
        else
        {
            i18n.send(context, POSITIVE, "There is now announcement named {}", announcement);
        }
    }

    @Command(desc = "modifies an announcement")
    public void modify(CommandCause context, Announcement announcement, @Greedy String message, @Named("locale") Locale locale, @Flag boolean append, @Flag boolean asJson)
    {
        message = message.replace("\\n", "\n");
        Component newText = asJson ? GsonComponentSerializer.gson().deserialize(message) : PlainComponentSerializer.plain().deserialize(message);
        if (locale == null)
        {
            Component prev = GsonComponentSerializer.gson().deserialize(append ? announcement.getConfig().announcement : "");
            announcement.getConfig().announcement = GsonComponentSerializer.gson().serialize(prev.append(newText));
        }
        else
        {
            Component prev = GsonComponentSerializer.gson().deserialize(append ? announcement.getConfig().translated.getOrDefault(locale, "") : "");
            announcement.getConfig().translated.put(locale, GsonComponentSerializer.gson().serialize(prev.append(newText)));
        }
        announcement.getConfig().save();
        i18n.send(context, POSITIVE, "Updated announcement");
    }
}
