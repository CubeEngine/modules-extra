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
package org.cubeengine.module.chat.command;

import java.sql.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Optional;
import de.cubeisland.engine.converter.ConversionException;
import de.cubeisland.engine.converter.node.StringNode;
import org.cubeengine.module.chat.Chat;
import org.cubeengine.module.chat.storage.Muted;
import org.cubeengine.module.core.util.TimeUtil;
import org.cubeengine.module.core.util.converter.DurationConverter;
import org.cubeengine.service.database.Database;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.user.UserManager;
import org.joda.time.Duration;
import org.jooq.types.UInteger;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.command.CommandSource;

import static java.util.concurrent.TimeUnit.DAYS;
import static org.cubeengine.module.chat.storage.TableMuted.TABLE_MUTED;
import static org.cubeengine.service.i18n.formatter.MessageType.*;

public class MuteCommands
{
    private Chat module;
    private Database db;
    private UserManager um;
    private I18n i18n;
    private final DurationConverter converter = new DurationConverter();

    private Map<UUID, java.util.Optional<Muted>> mutedMap = new HashMap<>();

    public MuteCommands(Chat module, Database db, UserManager um, I18n i18n)
    {
        this.module = module;
        this.db = db;
        this.um = um;
        this.i18n = i18n;
    }

    @Command(desc = "Mutes a player")
    public void mute(CommandSource context, Player player, @Optional String duration)
    {
        Date muted = getMuted(player);
        if (muted != null && muted.getTime() < System.currentTimeMillis())
        {
            i18n.sendTranslated(context, NEUTRAL, "{user} was already muted!", player);
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
                i18n.sendTranslated(context, NEGATIVE, "Invalid duration format!");
                return;
            }
        }

        setMuted(player, new Date(System.currentTimeMillis() + (dura.getMillis() == 0 ? DAYS.toMillis(
            9001) : dura.getMillis())));
        Text timeString = dura.getMillis() == 0 ? i18n.getTranslation(player, NONE, "ever") : Text.of(TimeUtil.format(
            player.getLocale(), dura.getMillis()));
        i18n.sendTranslated(player, NEGATIVE, "You are now muted for {input#amount}!", timeString);
        i18n.sendTranslated(context, NEUTRAL, "You muted {user} globally for {input#amount}!", player, timeString);
    }

    public Date getMuted(Player player)
    {
        if (!mutedMap.containsKey(player.getUniqueId()))
        {
            UInteger id = um.getByUUID(player.getUniqueId()).getEntity().getId();
            Muted muted = db.getDSL().selectFrom(TABLE_MUTED).where(TABLE_MUTED.ID.eq(id)).fetchOne();
            if (muted == null)
            {
                muted = db.getDSL().newRecord(TABLE_MUTED).newMuted(id);
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
