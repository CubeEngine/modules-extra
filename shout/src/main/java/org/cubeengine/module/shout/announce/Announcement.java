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
package org.cubeengine.module.shout.announce;

import java.util.Collection;
import java.util.Locale;
import org.cubeengine.module.shout.Shout;
import org.cubeengine.module.shout.ShoutUtil;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;

import static java.util.stream.Collectors.toList;
import static org.spongepowered.api.text.serializer.TextSerializers.FORMATTING_CODE;

/**
 * Class to represent an announcement.
 */
public class Announcement
{
    private final String name;
    private final Permission permission;
    private final AnnouncementConfig config;
    private long delay;

    public Announcement(Shout module, String name, AnnouncementConfig config, PermissionManager pm)
    {
        this.permission = "*".equals(config.permName) ? null : pm.register(module, config.permName, "", module.getAnnouncePerm());
        this.config = config;
        this.name = name;
        this.delay = ShoutUtil.parseDelay(config.delay) * 20 / 1000;
    }

    /**
     * Get the message from this announcement in a specified language
     *
     * @param   locale  The language to get the message in
     * @return	The message in that language if exist, else the message in the default locale will be returned
     */
    public Text getMessage(Locale locale)
    {
        String announcement = config.translated.getOrDefault(locale, config.announcement);
        return FORMATTING_CODE.deserialize(announcement);
    }

    /**
     * Get the delay
     *
     * @return The delay in milliseconds
     */
    public long getDelay()
    {
        return this.delay;
    }

    /**
     * Get the permission node for this announcement
     *
     * @return	the permission node for this announcement
     */
    public PermissionDescription getPermission()
    {
        return this.permission;
    }

    public String getName()
    {
        return this.name;
    }

    public boolean canAccess(CommandSource subject)
    {
        return getPermission() == null || subject.hasPermission(permission.getId());
    }

    protected Collection<CommandSource> getReceivers()
    {
        return MessageChannel.TO_ALL.getMembers().stream()
                                    .filter(m -> m instanceof CommandSource)
                                    .map(CommandSource.class::cast)
                                    .filter(this::canAccess)
                                    .collect(toList());
    }

    public void announce(CommandSource receiver)
    {
        receiver.sendMessages(getMessage(receiver.getLocale()));
    }

    public void announce()
    {
        getReceivers().forEach(this::announce);
    }
}
