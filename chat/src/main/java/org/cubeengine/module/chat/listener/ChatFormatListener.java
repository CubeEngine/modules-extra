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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.cubeengine.module.chat.Chat;
import org.cubeengine.module.chat.CubeMessageChannel;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.Game;
import org.spongepowered.api.data.manipulator.mutable.DisplayNameData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.option.OptionSubject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;

import static org.cubeengine.module.core.util.ChatFormat.fromLegacy;
import static org.spongepowered.api.text.format.TextColors.DARK_GREEN;

public class ChatFormatListener
{
    private final Chat module;
    private final Game game;
    private final I18n i18n;
    private static final Pattern chatColors = Pattern.compile("&[0123456789aAbBcCdDeEfFgkKlLmMnNoOrR]");

    public ChatFormatListener(Chat module, Game game, I18n i18n)
    {
        this.module = module;
        this.game = game;
        this.i18n = i18n;
    }

    @Listener
    public void onPlayerJoin(ClientConnectionEvent.Join event)
    {
        event.getTargetEntity().setMessageChannel(new CubeMessageChannel());
    }

    @Listener(order = Order.EARLY)
    public void onPlayerChat(MessageChannelEvent.Chat event, @First Player player)
    {
        // TODO format on the messagechannel instead
        String msg = event.getRawMessage().toPlain();
        if (module.getConfig().allowColors)
        {
            if (!player.hasPermission(module.perms().COLOR.getId()))
            {
                msg = chatColors.matcher(msg).replaceAll("");
            }
        }

        Subject subject = game.getServiceManager().provideUnchecked(PermissionService.class).getUserSubjects().get(
            player.getUniqueId().toString());

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
        if (subject instanceof OptionSubject)
        {
            replacements.put("{PREFIX}", fromLegacy(((OptionSubject)subject).getOption("chat-prefix").orElse(""), '&'));
            replacements.put("{SUFFIX}", fromLegacy(((OptionSubject)subject).getOption("chat-suffix").orElse(""), '&'));
        }

        event.setMessage(fromLegacy(this.getFormat(subject), replacements, '&'));
    }

    protected String getFormat(Subject subject)
    {
        String format = this.module.getConfig().format;
        if (subject instanceof OptionSubject)
        {
            format = ((OptionSubject)subject).getOption("chat-format").orElse(format);
        }
        return format;
    }
}
