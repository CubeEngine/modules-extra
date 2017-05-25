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
package org.cubeengine.module.squelch;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;

import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.squelch.command.IgnoreCommands;
import org.cubeengine.module.squelch.command.MuteCommands;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.text.channel.MessageReceiver;

import java.sql.Date;
import java.util.Iterator;

import javax.inject.Inject;

public class MuteListener
{
    private final IgnoreCommands ignoreCmd;
    private MuteCommands muteCmd;
    private I18n i18n;

    @Inject
    public MuteListener(IgnoreCommands ignore, MuteCommands muteCmd, I18n i18n)
    {
        this.ignoreCmd = ignore;
        this.muteCmd = muteCmd;
        this.i18n = i18n;
    }

    @Listener
    public void onChat(MessageChannelEvent.Chat event, @First Player source)
    {
        // muted?
        Date muted = muteCmd.getMuted(source);
        if (muted != null && System.currentTimeMillis() < muted.getTime())
        {
            event.setCancelled(true);
            i18n.sendTranslated(source, NEGATIVE, "You try to speak but nothing happens!");
            return;
        }
        // ignored?
        for (Iterator<MessageReceiver> iterator = event.getChannel().get().getMembers().iterator(); iterator.hasNext(); )
        {
            final MessageReceiver player = iterator.next();
            if (player instanceof Player)
            {
                if (this.ignoreCmd.checkIgnored(((Player) player), source))
                {
                    iterator.remove();
                }
            }
        }
    }
}
