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
package de.cubeisland.engine.module.holiday;

import java.sql.Date;
import java.text.DateFormat;

import de.cubeisland.engine.core.command.ContainerCommand;
import de.cubeisland.engine.core.command.context.CubeContext;
import de.cubeisland.engine.core.command.reflected.Command;
import de.cubeisland.engine.core.command.reflected.OnlyIngame;
import de.cubeisland.engine.core.command.reflected.context.Grouped;
import de.cubeisland.engine.core.command.reflected.context.IParams;
import de.cubeisland.engine.core.command.reflected.context.Indexed;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.converter.DurationConverter;
import de.cubeisland.engine.module.holiday.storage.HolidayModel;
import de.cubeisland.engine.reflect.exception.ConversionException;
import de.cubeisland.engine.reflect.node.StringNode;
import org.jooq.DSLContext;

import static de.cubeisland.engine.core.user.TableUser.TABLE_USER;
import static de.cubeisland.engine.core.util.formatter.MessageType.NEGATIVE;
import static de.cubeisland.engine.core.util.formatter.MessageType.NEUTRAL;
import static de.cubeisland.engine.core.util.formatter.MessageType.POSITIVE;
import static de.cubeisland.engine.module.holiday.storage.TableHoliday.TABLE_HOLIDAY;

public class HolidayCommands extends ContainerCommand
{
    private final DurationConverter converter = new DurationConverter();
    private final DSLContext dsl;

    public HolidayCommands(Holiday module)
    {
        super(module, "holiday", "Manages your holiday ");
        dsl = module.getCore().getDB().getDSL();
    }

    @Command(name = "for", desc = "Starts your holiday and kicks you from the server")
    @IParams({@Grouped(@Indexed(label = "duration")),
              @Grouped(value = @Indexed(label = "reason"), req = false, greedy = true)})
    @OnlyIngame
    public void forCommand(CubeContext context)
    {
        try
        {
            Date toDate = new Date(System.currentTimeMillis() + converter.fromNode(StringNode.of(context.getString(0)), null).getMillis());
            String reason = context.getStrings(1);
            User sender = (User)context.getSender();
            HolidayModel model = dsl.selectFrom(TABLE_HOLIDAY).where(TABLE_HOLIDAY.USERID.eq(sender.getEntity().getKey())).fetchOne();
            if (model == null)
            {
                model = dsl.newRecord(TABLE_HOLIDAY);
                model.setValue(TABLE_HOLIDAY.USERID, sender.getEntity().getKey());
            }
            model.setValue(TABLE_HOLIDAY.FROM, new Date(System.currentTimeMillis()));
            model.setValue(TABLE_HOLIDAY.TO, toDate);
            model.setValue(TABLE_HOLIDAY.REASON, reason);
            model.store();

            sender.getEntity().setValue(TABLE_USER.NOGC, false);
            sender.getEntity().update();

            sender.kickPlayer(sender.getTranslation(NEUTRAL, "You are now on holiday. See you later!"));
        }
        catch (ConversionException e)
        {
            context.sendTranslated(NEGATIVE, "Invalid duration format!");
        }
    }

    @Command(desc = "Checks the holiday status of a player")
    @IParams(@Grouped(@Indexed(label = "player", type = User.class)))
    public void check(CubeContext context)
    {
        User user = context.getArg(0);
        HolidayModel model = dsl.selectFrom(TABLE_HOLIDAY).where(TABLE_HOLIDAY.USERID.eq(
            user.getEntity().getKey())).fetchOne();
        if (model == null)
        {
            context.sendTranslated(NEUTRAL, "{user} is not on holiday!", user);
            return;
        }
        DateFormat df = DateFormat.getDateInstance(DateFormat.DEFAULT, context.getSender().getLocale());
        String dateTo = df.format(model.getValue(TABLE_HOLIDAY.TO));
        String dateFrom = df.format(model.getValue(TABLE_HOLIDAY.FROM));
        if (model.getValue(TABLE_HOLIDAY.TO).getTime() >= System.currentTimeMillis())
        {
            context.sendTranslated(POSITIVE, "{user} is on holiday from {input#date} to {input#date}", user, dateFrom, dateTo);
        }
        else
        {
            context.sendTranslated(POSITIVE, "{user} was on holiday from {input#date} to {input#date}", user, dateFrom, dateTo);
        }
        String reason = model.getValue(TABLE_HOLIDAY.REASON);
        if (reason != null)
        {
            context.sendTranslated(POSITIVE, "Reason: {input}", reason);
        }
    }
}
