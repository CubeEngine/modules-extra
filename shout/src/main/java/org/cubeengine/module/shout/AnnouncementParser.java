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

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;

import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.completer.Completer;
import org.cubeengine.butler.parameter.argument.ArgumentParser;
import org.cubeengine.butler.parameter.argument.ParserException;
import org.cubeengine.libcube.service.command.TranslatedParserException;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.shout.announce.Announcement;
import org.cubeengine.module.shout.announce.AnnouncementManager;
import org.spongepowered.api.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AnnouncementParser implements ArgumentParser<Announcement>, Completer
{
    private AnnouncementManager manager;
    private I18n i18n;

    public AnnouncementParser(AnnouncementManager manager, I18n i18n)
    {
        this.manager = manager;
        this.i18n = i18n;
    }

    @Override
    public Announcement parse(Class clazz, CommandInvocation invocation) throws ParserException
    {
        String name = invocation.consume(1);
        Announcement announcement = manager.getAnnouncement(name);
        if (announcement == null)
        {
            Text trans = i18n.getTranslation(invocation.getContext(Locale.class), NEGATIVE, "{input#announcement} was not found!", name);
            throw new TranslatedParserException(trans);
        }
        return announcement;
    }

    @Override
    public List<String> suggest(Class type, CommandInvocation invocation)
    {
        List<String> list = new ArrayList<>();
        String token = invocation.currentToken();
        for (Announcement announcement : manager.getAllAnnouncements())
        {
            if (announcement.getName().startsWith(token))
            {
                list.add(announcement.getName());
            }
        }
        return list;
    }
}
