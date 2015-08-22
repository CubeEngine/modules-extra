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
package de.cubeisland.engine.module.chat.listener;

import java.sql.Date;
import java.util.Iterator;
import de.cubeisland.engine.module.chat.Chat;
import de.cubeisland.engine.module.chat.ChatAttachment;
import de.cubeisland.engine.module.chat.command.IgnoreCommands;
import org.cubeengine.service.user.User;
import org.cubeengine.service.user.UserManager;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.entity.player.PlayerChatEvent;
import org.spongepowered.api.util.command.CommandSource;

import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;

public class MuteListener
{
    private final Chat module;
    private final IgnoreCommands ignore;
    private UserManager um;

    public MuteListener(Chat module, IgnoreCommands ignore, UserManager um)
    {
        this.module = module;
        this.ignore = ignore;
        this.um = um;
    }

    @Subscribe
    public void onChat(PlayerChatEvent event)
    {
        // muted?
        User sender = um.getExactUser(event.getUser().getUniqueId());
        if (sender != null)
        {
            ChatAttachment attachment = sender.attachOrGet(ChatAttachment.class, module);
            Date muted = attachment.getMuted();
            if (muted != null && System.currentTimeMillis() < muted.getTime())
            {
                event.setCancelled(true);
                sender.sendTranslated(NEGATIVE, "You try to speak but nothing happens!");
            }
        }
        // ignored?
        for (Iterator<CommandSource> iterator = event.getSink().getRecipients().iterator(); iterator.hasNext(); )
        {
            final CommandSource player = iterator.next();
            if (player instanceof Player)
            {
                User user = um.getExactUser(player.getName());
                if (this.ignore.checkIgnored(user, sender))
                {
                    iterator.remove();
                }
            }
        }
    }
}
