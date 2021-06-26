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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import com.google.inject.Inject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEvent.Action;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType;
import org.cubeengine.module.chat.Chat;
import org.cubeengine.module.chat.ChatConfig;
import org.cubeengine.module.chat.ChatPerm;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.message.PlayerChatEvent;
import org.spongepowered.api.service.permission.Subject;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.util.ComponentUtil.*;

public class ChatFormatListener
{
    private final Chat module;
    private I18n i18n;
    private ChatPerm perms;

    private Map<UUID, String> accumulated = new HashMap<>();
    private String format;
    private boolean allowColors;

    @Inject
    public ChatFormatListener(Chat module, I18n i18n, ChatPerm perms)
    {
        this.module = module;
        this.i18n = i18n;
        this.perms = perms;
    }

    public void init(ChatConfig config)
    {
        this.format = config.format;
        this.allowColors = config.allowColors;
    }

    @Listener(order = Order.EARLY)
    public void onPlayerChat(PlayerChatEvent event, @Root ServerPlayer player)
    {
        final PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();
        String msg = plainSerializer.serialize(event.originalMessage());

        if (!msg.equals("+") && msg.endsWith("+") && perms.LONGER.check(player))
        {
            msg = accumulated.getOrDefault(player.uniqueId(), "") + msg.substring(0, msg.length() - 1);
            msg = msg.substring(0, Math.min(msg.length(), 50 * 20));
            i18n.send(ChatType.ACTION_BAR, player, NEUTRAL, "{amount} characters in buffer.", msg.length());
            accumulated.put(player.uniqueId(), msg);
            event.setCancelled(true);
            return;
        }

        msg = accumulated.getOrDefault(player.uniqueId(), "") + msg;
        accumulated.remove(player.uniqueId());
        msg = msg.substring(0, Math.min(msg.length(), 50 * 20));

        if (this.allowColors)
        {
            if (!player.hasPermission(perms.COLOR.getId()))
            {
                msg = stripLegacy(msg);
            }
        }

        if (player.hasPermission(perms.NEWLINE.getId()))
        {
            msg = msg.replace("\\n", "\n");
        }

        try
        {
            Subject subject = module.getPermissionService().userSubjects().loadSubject(player.uniqueId().toString()).get();
            event.setMessage(fromLegacy(msg));
            event.setChatFormatter((p, audience, message, originalMessage) ->
                   Optional.of(legacyMessageTemplateToComponent(this.getFormat(subject), getReplacements(player, message, subject))));
        }
        catch (ExecutionException | InterruptedException e)
        {
            throw new IllegalStateException(e);
        }
    }

    @NotNull
    private Map<String, Component> getReplacements(ServerPlayer player, Component message, Subject subject)
    {
        String name = player.name();
        final PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();
        Map<String, Component> replacements = new HashMap<>();
        replacements.put("NAME", Component.text(name));
        Component displayName = player.get(Keys.CUSTOM_NAME).orElse(Component.text(name));
        if (!plainSerializer.serialize(displayName).equals(name))
        {
            final HoverEvent<Component> hoverEvent = HoverEvent.hoverEvent(Action.SHOW_TEXT, Component.text(name).color(NamedTextColor.DARK_GREEN));
            displayName = Component.text().append(displayName).hoverEvent(hoverEvent).build();
        }
        replacements.put("DISPLAY_NAME", displayName);
        replacements.put("MESSAGE", message);
        replacements.put("WORLD", Component.text(player.world().properties().key().toString()));
        replacements.put("PREFIX", fromLegacy(subject.option("chat-prefix").orElse("")));
        replacements.put("SUFFIX", fromLegacy(subject.option("chat-suffix").orElse("")));
        return replacements;
    }

    protected String getFormat(Subject subject)
    {
        return subject.option("chat-format").orElse(this.format);
    }
}
