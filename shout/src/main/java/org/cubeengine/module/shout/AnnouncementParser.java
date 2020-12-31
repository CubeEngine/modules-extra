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
package org.cubeengine.module.shout;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.cubeengine.libcube.service.command.annotation.ParserFor;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.shout.announce.Announcement;
import org.cubeengine.module.shout.announce.AnnouncementManager;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.parameter.ArgumentReader.Mutable;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.CommandContext.Builder;
import org.spongepowered.api.command.parameter.Parameter.Key;
import org.spongepowered.api.command.parameter.managed.ValueCompleter;
import org.spongepowered.api.command.parameter.managed.ValueParser;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;

@ParserFor(Announcement.class)
public class AnnouncementParser implements ValueParser<Announcement>, ValueCompleter
{
    private AnnouncementManager manager;
    private I18n i18n;

    public AnnouncementParser(AnnouncementManager manager, I18n i18n)
    {
        this.manager = manager;
        this.i18n = i18n;
    }

    @Override
    public List<String> complete(CommandContext context, String currentInput)
    {
        List<String> list = new ArrayList<>();
        String token = currentInput;
        for (Announcement announcement : manager.getAllAnnouncements())
        {
            if (announcement.getName().startsWith(token))
            {
                list.add(announcement.getName());
            }
        }
        return list;
    }

    @Override
    public Optional<? extends Announcement> getValue(Key<? super Announcement> parameterKey, Mutable reader, Builder context) throws ArgumentParseException
    {
        String name = reader.parseString();
        Announcement announcement = manager.getAnnouncement(name);
        if (announcement == null)
        {
            throw reader.createException(i18n.translate(context.getCause(), NEGATIVE, "{input#announcement} was not found!", name));
        }
        return Optional.of(announcement);
    }


}
