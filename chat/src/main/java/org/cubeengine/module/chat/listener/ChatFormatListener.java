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
package org.cubeengine.module.chat.listener;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.libcube.util.ChatFormat.fromLegacy;
import static org.spongepowered.api.text.format.TextColors.DARK_GREEN;

import org.cubeengine.libcube.service.i18n.formatter.MessageType;
import org.cubeengine.module.chat.Chat;
import org.spongepowered.api.data.manipulator.mutable.DisplayNameData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.chat.ChatTypes;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import javax.inject.Inject;

public class ChatFormatListener
{
    private final Chat module;
    private static final Pattern chatColors = Pattern.compile("&[0123456789aAbBcCdDeEfFgkKlLmMnNoOrR]");

    private Map<UUID, String> accumulated = new HashMap<>();

    @Inject
    public ChatFormatListener(Chat module)
    {
        this.module = module;
    }

    @Listener(order = Order.EARLY)
    public void onPlayerChat(MessageChannelEvent.Chat event, @Root Player player)
    {
        // TODO format on the messagechannel instead
        String msg = event.getRawMessage().toPlain();

        if (!msg.equals("+") && msg.endsWith("+") && player.hasPermission(module.perms().LONGER.getId()))
        {
            msg = accumulated.getOrDefault(player.getUniqueId(), "") + msg.substring(0, msg.length() - 1);
            msg = msg.substring(0, Math.min(msg.length(), 50 * 20));
            module.getI18n().send(ChatTypes.ACTION_BAR, player, NEUTRAL,"{amount} characters in buffer.", msg.length());
            accumulated.put(player.getUniqueId(), msg);
            event.setCancelled(true);
            return;
        }

        msg = accumulated.getOrDefault(player.getUniqueId(), "") + msg;
        accumulated.remove(player.getUniqueId());
        msg = msg.substring(0, Math.min(msg.length(), 50 * 20));

        if (module.getConfig().allowColors)
        {
            if (!player.hasPermission(module.perms().COLOR.getId()))
            {
                msg = chatColors.matcher(msg).replaceAll("");
            }
        }

        if (player.hasPermission(module.perms().NEWLINE.getId()))
        {
            msg = msg.replace("\\n", "\n");
        }

        try
        {
            Subject subject = module.getPermissionService().getUserSubjects().loadSubject(player.getUniqueId().toString()).get();

            Map<String, Text> replacements = new HashMap<>();
            String name = player.getName();
            replacements.put("{NAME}", Text.of(name));
            Text displayName = player.get(DisplayNameData.class).isPresent() ?
                    player.getDisplayNameData().displayName().get() : Text.of(name);
            if (!displayName.toPlain().equals(name))
            {
                displayName = Text.builder().append(displayName).onHover(TextActions.showText(Text.of(DARK_GREEN, name))).build();
            }
            replacements.put("{DISPLAY_NAME}", displayName);
            replacements.put("{WORLD}", Text.of(player.getWorld().getName()));
            replacements.put("{MESSAGE}", fromLegacy(msg, '&'));
            replacements.put("{PREFIX}", Text.of());
            replacements.put("{SUFFIX}", Text.of());
            replacements.put("{PREFIX}", fromLegacy(subject.getOption("chat-prefix").orElse(""), '&'));
            replacements.put("{SUFFIX}", fromLegacy(subject.getOption("chat-suffix").orElse(""), '&'));

            event.setMessage(fromLegacy(this.getFormat(subject), replacements, '&'));
        }
        catch (ExecutionException | InterruptedException e)
        {
            throw new IllegalStateException(e);
        }
    }

    protected String getFormat(Subject subject)
    {
        return subject.getOption("chat-format").orElse(this.module.getConfig().format);
    }
}
