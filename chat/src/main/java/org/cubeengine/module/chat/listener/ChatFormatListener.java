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
import java.util.Optional;
import org.cubeengine.module.chat.Chat;
import org.cubeengine.module.chat.CubeMessageSink;
import org.cubeengine.service.i18n.I18n;
import org.spongepowered.api.Game;
import org.spongepowered.api.data.manipulator.mutable.DisplayNameData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.command.MessageSinkEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.option.OptionSubject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.module.core.util.ChatFormat.fromLegacy;
import static org.cubeengine.service.i18n.formatter.MessageType.NEUTRAL;
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
        event.getTargetEntity().setMessageSink(new CubeMessageSink());
    }

    @Listener(order = Order.EARLY)
    public void onPlayerChat(MessageSinkEvent.Chat event, @First Player player)
    {
        String msg = Texts.toPlain(event.getRawMessage());
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
        replacements.put("{NAME}", Texts.of(name));
        Text displayName = player.get(DisplayNameData.class).isPresent() ?
            player.getDisplayNameData().displayName().get() : Texts.of(name);
        if (!Texts.toPlain(displayName).equals(name))
        {
            displayName = Texts.builder().append(displayName).onHover(TextActions.showText(Texts.of(DARK_GREEN, name))).build();
        }
        replacements.put("{DISPLAY_NAME}", displayName);
        replacements.put("{WORLD}", Texts.of(player.getWorld().getName()));
        replacements.put("{MESSAGE}", fromLegacy(msg, '&'));
        replacements.put("{PREFIX}", Texts.of());
        replacements.put("{SUFFIX}", Texts.of());
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
