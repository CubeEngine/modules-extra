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
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Optional;
import de.cubeisland.engine.converter.ConversionException;
import de.cubeisland.engine.converter.node.StringNode;
import org.cubeengine.module.chat.Chat;
import org.cubeengine.module.chat.storage.Muted;
import org.cubeengine.module.core.util.TimeUtil;
import org.cubeengine.module.core.util.converter.DurationConverter;
import org.cubeengine.service.database.Database;
import org.cubeengine.service.user.MultilingualCommandSource;
import org.cubeengine.service.user.MultilingualPlayer;
import org.cubeengine.service.user.UserManager;
import org.joda.time.Duration;
import org.jooq.types.UInteger;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;

import static java.util.concurrent.TimeUnit.DAYS;
import static org.cubeengine.module.chat.storage.TableMuted.TABLE_MUTED;
import static org.cubeengine.service.i18n.formatter.MessageType.*;

public class MuteCommands
{
    private Chat module;
    private Database db;
    private UserManager um;
    private final DurationConverter converter = new DurationConverter();

    private Map<UUID, java.util.Optional<Muted>> mutedMap = new HashMap<>();

    public MuteCommands(Chat module, Database db, UserManager um)
    {
        this.module = module;
        this.db = db;
        this.um = um;
    }

    @Command(desc = "Mutes a player")
    public void mute(MultilingualCommandSource context, MultilingualPlayer player, @Optional String duration)
    {
        Date muted = getMuted(player.getSource());
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

        setMuted(player.getSource(), new Date(System.currentTimeMillis() + (dura.getMillis() == 0 ? DAYS.toMillis(
            9001) : dura.getMillis())));
        Text timeString = dura.getMillis() == 0 ? player.getTranslation(NONE, "ever") : Texts.of(TimeUtil.format(player.getSource().getLocale(), dura.getMillis()));
        player.sendTranslated(NEGATIVE, "You are now muted for {input#amount}!", timeString);
        context.sendTranslated(NEUTRAL, "You muted {user} globally for {input#amount}!", player, timeString);
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
    public void unmute(MultilingualCommandSource context, MultilingualPlayer player)
    {
        setMuted(player.getSource(), null);
        context.sendTranslated(POSITIVE, "{user} is no longer muted!", player);
    }
}
