package org.cubeengine.module.shout;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;

import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.completer.Completer;
import org.cubeengine.butler.parameter.reader.ArgumentReader;
import org.cubeengine.butler.parameter.reader.ReaderException;
import org.cubeengine.libcube.service.command.TranslatedReaderException;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.shout.announce.Announcement;
import org.cubeengine.module.shout.announce.AnnouncementManager;
import org.spongepowered.api.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AnnouncementReader implements ArgumentReader<Announcement>, Completer
{
    private AnnouncementManager manager;
    private I18n i18n;

    public AnnouncementReader(AnnouncementManager manager, I18n i18n)
    {
        this.manager = manager;
        this.i18n = i18n;
    }

    @Override
    public Announcement read(Class clazz, CommandInvocation invocation) throws ReaderException
    {
        String name = invocation.consume(1);
        Announcement announcement = manager.getAnnouncement(name);
        if (announcement == null)
        {
            Text trans = i18n.getTranslation(invocation.getContext(Locale.class), NEGATIVE, "{input#announcement} was not found!", name);
            throw new TranslatedReaderException(trans);
        }
        return announcement;
    }

    @Override
    public List<String> getSuggestions(CommandInvocation invocation)
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
