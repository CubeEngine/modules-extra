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
package org.cubeengine.module.holiday;

import java.sql.Date;
import java.text.DateFormat;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Greed;
import org.cubeengine.butler.parametric.Optional;
import de.cubeisland.engine.converter.ConversionException;
import de.cubeisland.engine.converter.node.StringNode;
import org.cubeengine.libcube.service.command.ContainerCommand;
import org.cubeengine.module.holiday.storage.HolidayModel;
import org.cubeengine.libcube.service.converter.DurationConverter;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.i18n.formatter.MessageType;
import org.jooq.DSLContext;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;

import static org.cubeengine.butler.parameter.Parameter.INFINITE;
import static org.cubeengine.module.holiday.storage.TableHoliday.TABLE_HOLIDAY;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEUTRAL;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

@Command(name = "holiday", desc = "Manages your holiday ")
public class HolidayCommands extends ContainerCommand
{
    private final DurationConverter converter = new DurationConverter();
    private final DSLContext dsl;
    private I18n i18n;

    public HolidayCommands(Holiday module, DSLContext dsl, I18n i18n)
    {
        super(module);
        this.dsl = dsl;
        this.i18n = i18n;
    }

    @Command(name = "for", desc = "Starts your holiday and kicks you from the server")
    @Restricted(User.class)
    public void forCommand(Player context, String duration, @Optional @Greed(INFINITE) String reason)
    {
        try
        {
            Date toDate = new Date(System.currentTimeMillis() + converter.fromNode(StringNode.of(duration)).getMillis());
            HolidayModel model = dsl.selectFrom(TABLE_HOLIDAY).where(TABLE_HOLIDAY.USERID.eq(
                context.getUniqueId())).fetchOne();
            if (model == null)
            {
                model = dsl.newRecord(TABLE_HOLIDAY);
                model.setValue(TABLE_HOLIDAY.USERID, context.getUniqueId());
            }
            model.setValue(TABLE_HOLIDAY.FROM, new Date(System.currentTimeMillis()));
            model.setValue(TABLE_HOLIDAY.TO, toDate);
            model.setValue(TABLE_HOLIDAY.REASON, reason);
            model.store();

            context.kick(i18n.getTranslation(context, NEUTRAL, "You are now on holiday. See you later!"));
        }
        catch (ConversionException e)
        {
            i18n.sendTranslated(context, NEGATIVE, "Invalid duration format!");
        }
    }

    @Command(desc = "Checks the holiday status of a player")
    public void check(CommandSource context, User player)
    {
        HolidayModel model = dsl.selectFrom(TABLE_HOLIDAY).where(TABLE_HOLIDAY.USERID.eq(player.getUniqueId())).fetchOne();
        if (model == null)
        {
            i18n.sendTranslated(context, NEUTRAL, "{user} is not on holiday!", player);
            return;
        }
        DateFormat df = DateFormat.getDateInstance(DateFormat.DEFAULT, context.getLocale());
        String dateTo = df.format(model.getValue(TABLE_HOLIDAY.TO));
        String dateFrom = df.format(model.getValue(TABLE_HOLIDAY.FROM));
        if (model.getValue(TABLE_HOLIDAY.TO).getTime() >= System.currentTimeMillis())
        {
            i18n.sendTranslated(context, POSITIVE, "{user} is on holiday from {input#date} to {input#date}", player, dateFrom, dateTo);
        }
        else
        {
            i18n.sendTranslated(context, POSITIVE, "{user} was on holiday from {input#date} to {input#date}", player, dateFrom, dateTo);
        }
        String reason = model.getValue(TABLE_HOLIDAY.REASON);
        if (reason != null)
        {
            i18n.sendTranslated(context, POSITIVE, "Reason: {input}", reason);
        }
    }
}
