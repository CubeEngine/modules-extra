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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import de.cubeisland.engine.module.chat.Chat;
import de.cubeisland.engine.module.chat.CubeMessageSink;
import de.cubeisland.engine.service.i18n.I18n;
import org.spongepowered.api.Game;
import org.spongepowered.api.data.manipulator.mutable.DisplayNameData;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.entity.player.PlayerChatEvent;
import org.spongepowered.api.event.entity.player.PlayerJoinEvent;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.option.OptionSubject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Text.Translatable;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.action.HoverAction.ShowText;

import static de.cubeisland.engine.module.core.util.ChatFormat.fromLegacy;
import static de.cubeisland.engine.service.i18n.formatter.MessageType.NEUTRAL;
import static org.spongepowered.api.text.Texts.toPlain;


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

    @Subscribe
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        event.getUser().setMessageSink(new CubeMessageSink());
    }

    @Subscribe(order = Order.EARLY)
    public void onPlayerChat(PlayerChatEvent event)
    {
        String msg = Texts.toPlain(event.getUnformattedMessage());
        if (module.getConfig().allowColors)
        {
            if (!event.getUser().hasPermission(module.perms().COLOR.getId()))
            {
                msg = chatColors.matcher(msg).replaceAll("");
            }
        }

        Subject subject = game.getServiceManager().provideUnchecked(PermissionService.class).getUserSubjects().get(
            event.getUser().getUniqueId().toString());

        Map<String, Text> replacements = new HashMap<>();
        String name = event.getUser().getName();
        replacements.put("{NAME}", Texts.of(name));
        Text displayName = event.getUser().get(DisplayNameData.class).isPresent() ?
            event.getUser().getDisplayNameData().displayName().get() : Texts.of(name);
        if (!Texts.toPlain(displayName).equals(name))
        {
            Text translation = i18n.getTranslation(null, NEUTRAL, "Actual name: {user}", name);
            displayName = Texts.builder().append(displayName).onHover(new ShowText(translation)).build();
        }
        replacements.put("{DISPLAY_NAME}", displayName);
        replacements.put("{WORLD}", Texts.of(event.getUser().getWorld().getName()));
        replacements.put("{MESSAGE}", fromLegacy(msg, '&'));
        replacements.put("{PREFIX}", Texts.of());
        replacements.put("{SUFFIX}", Texts.of());
        if (subject instanceof OptionSubject)
        {
            replacements.put("{PREFIX}", fromLegacy(((OptionSubject)subject).getOption("chat-prefix").or(""), '&'));
            replacements.put("{SUFFIX}", fromLegacy(((OptionSubject)subject).getOption("chat-suffix").or(""), '&'));
        }

        event.setNewMessage(fromLegacy(this.getFormat(subject), replacements, '&'));
    }

    protected String getFormat(Subject subject)
    {
        String format = this.module.getConfig().format;
        if (subject instanceof OptionSubject)
        {
            format = ((OptionSubject)subject).getOption("chat-format").or(format);
        }
        return format;
    }
}
