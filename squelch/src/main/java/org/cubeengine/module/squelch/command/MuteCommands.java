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
package org.cubeengine.module.squelch.command;

import static java.util.concurrent.TimeUnit.DAYS;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NONE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.module.squelch.storage.TableMuted.TABLE_MUTED;

import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Optional;
import org.cubeengine.converter.ConversionException;
import org.cubeengine.converter.node.StringNode;
import org.cubeengine.libcube.service.config.DurationConverter;
import org.cubeengine.libcube.service.database.Database;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.TimeUtil;
import org.cubeengine.module.squelch.Squelch;
import org.cubeengine.module.squelch.storage.Muted;
import org.joda.time.Duration;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

import java.sql.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

public class  MuteCommands
{
    private Squelch module;
    private Database db;
    private I18n i18n;
    private final DurationConverter converter = new DurationConverter();

    private Map<UUID, java.util.Optional<Muted>> mutedMap = new HashMap<>();

    @Inject
    public MuteCommands(Squelch module, Database db, I18n i18n)
    {
        this.module = module;
        this.db = db;
        this.i18n = i18n;
    }

    @Command(desc = "Mutes a player")
    public void mute(CommandSource context, Player player, @Optional String duration)
    {
        Date muted = getMuted(player);
        if (muted != null && muted.getTime() > System.currentTimeMillis())
        {
            i18n.sendTranslated(context, NEUTRAL, "{user} was already muted!", player);
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
                i18n.sendTranslated(context, NEGATIVE, "Invalid duration format!");
                return;
            }
        }

        setMuted(player, new Date(System.currentTimeMillis() + (dura.getMillis() == 0 ? DAYS.toMillis(
            9001) : dura.getMillis())));
        Text timeString = dura.getMillis() == 0 ? i18n.getTranslation(player, NONE, "ever") :
                Text.of(TimeUtil.format(player.getLocale(), dura.getMillis()));
        i18n.sendTranslated(player, NEGATIVE, "You are now muted for {txt#amount}!", timeString);
        i18n.sendTranslated(context, NEUTRAL, "You muted {user} globally for {txt#amount}!", player, timeString);
    }

    public Date getMuted(Player player)
    {
        if (!mutedMap.containsKey(player.getUniqueId()))
        {
            Muted muted = db.getDSL().selectFrom(TABLE_MUTED).where(TABLE_MUTED.ID.eq(player.getUniqueId())).fetchOne();
            if (muted == null)
            {
                muted = db.getDSL().newRecord(TABLE_MUTED).newMuted(player.getUniqueId());
            }
            mutedMap.put(player.getUniqueId(), java.util.Optional.of(muted));
        }
        return mutedMap.get(player.getUniqueId()).map(d -> d.getValue(TABLE_MUTED.MUTED)).orElse(null);
    }

    public void setMuted(Player player, Date date)
    {
        getMuted(player);
        Muted muted = mutedMap.get(player.getUniqueId()).get();
        muted.setValue(TABLE_MUTED.MUTED, date);
        muted.storeAsync();
    }


    @Command(desc = "Unmutes a player")
    public void unmute(CommandSource context, Player player)
    {
        setMuted(player, null);
        i18n.sendTranslated(context, POSITIVE, "{user} is no longer muted!", player);
    }
}
