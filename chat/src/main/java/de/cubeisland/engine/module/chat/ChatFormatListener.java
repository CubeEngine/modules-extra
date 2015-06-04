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
package de.cubeisland.engine.module.chat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import de.cubeisland.engine.module.core.i18n.I18n;
import de.cubeisland.engine.module.core.util.ChatFormat;
import org.spongepowered.api.Game;
import org.spongepowered.api.data.manipulator.DisplayNameData;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.entity.player.PlayerChatEvent;
import org.spongepowered.api.event.entity.player.PlayerJoinEvent;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.option.OptionSubject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Text.Translatable;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.action.HoverAction.ShowText;
import org.spongepowered.api.text.format.BaseFormatting;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyle;
import org.spongepowered.api.text.format.TextStyle.Base;
import org.spongepowered.api.text.translation.Translation;

import static de.cubeisland.engine.module.core.util.formatter.MessageType.NEUTRAL;
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
        if (!(event.getMessage() instanceof Translatable))
        {
            return;
        }
        String msg = toPlain((Text)((Translatable)event.getMessage()).getArguments().get(1));
        if (module.getConfig().allowColors)
        {
            if (!event.getUser().hasPermission(module.perms().COLOR.getFullName()))
            {
                msg = chatColors.matcher(msg).replaceAll("");
            }
        }

        Subject subject = game.getServiceManager().provideUnchecked(PermissionService.class).getUserSubjects().get(
            event.getUser().getUniqueId().toString());

        Map<String, Text> replacements = new HashMap<>();
        String name = event.getUser().getName();
        replacements.put("{NAME}", Texts.of(name));
        Text displayName = event.getUser().getData(DisplayNameData.class).isPresent() ?
            event.getUser().getDisplayNameData().getDisplayName() : Texts.of(name);
        if (!Texts.toPlain(displayName).equals(name))
        {
            Translation translation = i18n.getTranslation(NEUTRAL, null, "Actual name: {user}", name);
            displayName = Texts.builder().append(displayName).onHover(new ShowText(Texts.of(translation))).build();
        }
        replacements.put("{DISPLAY_NAME}", displayName);
        replacements.put("{WORLD}", Texts.of(event.getUser().getWorld().getName()));
        replacements.put("{MESSAGE}", fromLegacy(msg));
        if (subject instanceof OptionSubject)
        {
            replacements.put("{PREFIX}", fromLegacy(((OptionSubject)subject).getOption("chat-prefix").or("")));
            replacements.put("{SUFFIX}", fromLegacy(((OptionSubject)subject).getOption("chat-suffix").or("")));
        }

        event.setNewMessage(fromLegacy(this.getFormat(subject), replacements));
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

    private Text fromLegacy(String string, Map<String, Text> replacements)
    {
        String[] parts = string.split(
            "((?<=&[0123456789aAbBcCdDeEfFgkKlLmMnNoOrR])|(?=&[0123456789aAbBcCdDeEfFgkKlLmMnNoOrR]))");
        TextBuilder builder = Texts.builder();
        TextColor nextColor = null;
        TextStyle nextStyle = null;
        for (String part : parts)
        {
            if (part.matches("&[0123456789aAbBcCdDeEfFg]"))
            {
                nextColor = ((TextColor)ChatFormat.getByChar(part.charAt(1)).getBase());
                continue;
            }
            if (part.matches("&[kKlLmMnNoOrR]"))
            {
                TextStyle newStyle = (TextStyle)ChatFormat.getByChar(part.charAt(1)).getBase();
                if (nextStyle == null)
                {
                    nextStyle = newStyle;
                }
                else
                {
                    nextStyle = nextStyle.and(newStyle);
                }
                continue;
            }

            TextBuilder partBuilder = Texts.builder();
            String[] toReplace = part.split("((?<=\\{[A-Z_]{0,50}\\})|(?=\\{[A-Z_]{0,50}\\}))");
            for (String r : toReplace)
            {
                Text text = replacements.get(r);
                if (text != null)
                {
                    partBuilder.append(text);
                }
                else if (!r.matches("\\{.+\\}"))
                {
                    partBuilder.append(Texts.of(r));
                }
            }
            if (nextColor != null)
            {
                partBuilder.color(nextColor);
                nextColor = null;
            }
            if (nextStyle != null)
            {
                partBuilder.style(nextStyle);
                nextStyle = null;
            }

            builder.append(partBuilder.build());
        }
        return builder.build();
    }

    private Text fromLegacy(String string)
    {
        return fromLegacy(string, Collections.emptyMap());
    }
}
