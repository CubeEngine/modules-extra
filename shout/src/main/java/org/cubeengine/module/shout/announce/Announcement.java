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
package org.cubeengine.module.shout.announce;

import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;
import com.google.gson.JsonParseException;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.shout.Shout;
import org.cubeengine.module.shout.ShoutUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

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
        this.permission = "*".equals(config.permName) ? null : pm.register(Shout.class, config.permName, "", module.getAnnouncePerm());
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
    public Component getMessage(Locale locale)
    {
        String announcement = config.translated.getOrDefault(locale, config.announcement);
        try {
            return GsonComponentSerializer.gson().deserialize(announcement);
        } catch (JsonParseException e) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(announcement);
        }
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
    public Permission getPermission()
    {
        return this.permission;
    }

    public String getName()
    {
        return this.name;
    }

    public boolean canAccess(ServerPlayer subject)
    {
        return getPermission() == null || subject.hasPermission(permission.getId());
    }

    protected Collection<ServerPlayer> getReceivers()
    {
        return Sponge.getServer().getOnlinePlayers().stream()
              .filter(this::canAccess)
              .collect(Collectors.toList());
    }

    public void announce(ServerPlayer receiver)
    {
        receiver.sendMessage(Identity.nil(), getMessage(receiver.getLocale()));
    }

    public void announce()
    {
        getReceivers().forEach(this::announce);
    }

    public void delete() {
        this.config.getFile().delete();
    }

    public int weight() {
        return this.config.weight;
    }

    public AnnouncementConfig getConfig() {
        return config;
    }
}
