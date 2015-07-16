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
package de.cubeisland.engine.module.chat.command;

import java.sql.Date;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Optional;
import de.cubeisland.engine.converter.ConversionException;
import de.cubeisland.engine.converter.node.StringNode;
import de.cubeisland.engine.module.chat.Chat;
import de.cubeisland.engine.module.chat.ChatAttachment;
import de.cubeisland.engine.module.core.util.TimeUtil;
import de.cubeisland.engine.module.core.util.converter.DurationConverter;
import de.cubeisland.engine.service.command.CommandSender;
import de.cubeisland.engine.service.user.User;
import org.joda.time.Duration;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;

import static de.cubeisland.engine.service.i18n.formatter.MessageType.*;
import static java.util.concurrent.TimeUnit.DAYS;

public class MuteCommands
{
    private Chat module;
    private final DurationConverter converter = new DurationConverter();


    public MuteCommands(Chat module)
    {
        this.module = module;
    }

    @Command(desc = "Mutes a player")
    public void mute(CommandSender context, User player, @Optional String duration)
    {
        ChatAttachment attachment = player.attachOrGet(ChatAttachment.class, module);
        Date muted = attachment.getMuted();
        if (muted != null && muted.getTime() < System.currentTimeMillis())
        {
            context.sendTranslated(NEUTRAL, "{user} was already muted!", player);
        }
        Duration dura = module.getConfig().defaultMuteTime;
        if (duration != null)
        {
            try
            {
                dura = converter.fromNode(StringNode.of(duration), null, null);
            }
            catch (ConversionException e)
            {
                context.sendTranslated(NEGATIVE, "Invalid duration format!");
                return;
            }
        }
        attachment.setMuted(new Date(System.currentTimeMillis() + (dura.getMillis() == 0 ? DAYS.toMillis(
            9001) : dura.getMillis())));
        Text timeString = dura.getMillis() == 0 ? player.getTranslation(NONE, "ever") : Texts.of(TimeUtil.format(player.getLocale(), dura.getMillis()));
        player.sendTranslated(NEGATIVE, "You are now muted for {input#amount}!", timeString);
        context.sendTranslated(NEUTRAL, "You muted {user} globally for {input#amount}!", player, timeString);
    }

    @Command(desc = "Unmutes a player")
    public void unmute(CommandSender context, User player)
    {
        ChatAttachment attachment = player.attachOrGet(ChatAttachment.class, module);
        attachment.setMuted(null);
        context.sendTranslated(POSITIVE, "{user} is no longer muted!", player);
    }
}
