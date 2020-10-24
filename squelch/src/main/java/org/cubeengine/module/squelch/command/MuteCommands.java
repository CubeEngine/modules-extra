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
package org.cubeengine.module.squelch.command;

import java.sql.Date;
import java.time.Duration;
import com.google.inject.Inject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import org.cubeengine.converter.ConversionException;
import org.cubeengine.converter.node.StringNode;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Option;
import org.cubeengine.libcube.service.config.DurationConverter;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.TimeUtil;
import org.cubeengine.module.squelch.Squelch;
import org.cubeengine.module.squelch.data.SquelchData;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.Player;

import static java.util.concurrent.TimeUnit.DAYS;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

public class  MuteCommands
{
    private Squelch module;
    private I18n i18n;
    private final DurationConverter converter = new DurationConverter();

    @Inject
    public MuteCommands(Squelch module, I18n i18n)
    {
        this.module = module;
        this.i18n = i18n;
    }

    @Command(desc = "Mutes a player")
    public void mute(CommandCause context, Player player, @Option String duration)
    {
        Date muted = getMuted(player);
        if (muted != null && muted.getTime() > System.currentTimeMillis())
        {
            i18n.send(context.getAudience(), NEUTRAL, "{user} was already muted!", player);
        }
        try
        {
            Integer.parseInt(duration);
            duration += "m";
        }
        catch (NumberFormatException ignored)
        {}
        Duration dura = module.getConfig().defaultMuteTime;
        if (duration != null)
        {
            try
            {
                dura = converter.fromNode(StringNode.of(duration), null, null);
            }
            catch (ConversionException e)
            {
                i18n.send(context.getAudience(), NEGATIVE, "Invalid duration format!");
                return;
            }
        }

        setMuted(player, new Date(System.currentTimeMillis() + (dura.toMillis() == 0 ? DAYS.toMillis(9001) : dura.toMillis())));
        Component timeString = dura.toMillis() == 0 ? i18n.translate(player, Style.empty(), "ever") :
                               Component.text(TimeUtil.format(player.getLocale(), dura.toMillis()));
        i18n.send(player, NEGATIVE, "You are now muted for {txt#amount}!", timeString);
        i18n.send(context.getAudience(), NEUTRAL, "You muted {user} globally for {txt#amount}!", player, timeString);
    }

    public Date getMuted(Player player)
    {
        return player.get(SquelchData.MUTED).map(Date::new).orElse(null);
    }

    public void setMuted(Player player, Date date)
    {
        player.offer(SquelchData.MUTED, date.getTime());
    }

    @Command(desc = "Unmutes a player")
    public void unmute(CommandCause context, Player player)
    {
        player.remove(SquelchData.MUTED);
        i18n.send(context.getAudience(), POSITIVE, "{user} is no longer muted!", player);
    }
}
