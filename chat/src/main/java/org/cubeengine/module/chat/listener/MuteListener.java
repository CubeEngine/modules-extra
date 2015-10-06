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
package org.cubeengine.module.chat.listener;

import java.sql.Date;
import java.util.Iterator;
import java.util.Optional;
import org.cubeengine.module.chat.command.IgnoreCommands;
import org.cubeengine.module.chat.command.MuteCommands;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.command.MessageSinkEvent;
import org.spongepowered.api.util.command.CommandSource;

import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;

public class MuteListener
{
    private final IgnoreCommands ignoreCmd;
    private MuteCommands muteCmd;
    private I18n i18n;

    public MuteListener(IgnoreCommands ignore, MuteCommands muteCmd, I18n i18n)
    {
        this.ignoreCmd = ignore;
        this.muteCmd = muteCmd;
        this.i18n = i18n;
    }

    @Listener
    public void onChat(MessageSinkEvent event)
    {
        Optional<Player> source = event.getCause().first(Player.class);
        if (!source.isPresent())
        {
            return;
        }
        // muted?
        Date muted = muteCmd.getMuted(source.get());
        if (muted != null && System.currentTimeMillis() < muted.getTime())
        {
            event.setCancelled(true);
            i18n.sendTranslated(source.get(), NEGATIVE, "You try to speak but nothing happens!");
            return;
        }
        // ignored?
        for (Iterator<CommandSource> iterator = event.getSink().getRecipients().iterator(); iterator.hasNext(); )
        {
            final CommandSource player = iterator.next();
            if (player instanceof Player)
            {
                if (this.ignoreCmd.checkIgnored(((Player)player), source.get()))
                {
                    iterator.remove();
                }
            }
        }
    }
}
