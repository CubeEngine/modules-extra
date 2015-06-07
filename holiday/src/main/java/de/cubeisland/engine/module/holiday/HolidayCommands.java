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
import de.cubeisland.engine.butler.filter.Restricted;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Greed;
import de.cubeisland.engine.butler.parametric.Optional;
import de.cubeisland.engine.converter.ConversionException;
import de.cubeisland.engine.converter.node.StringNode;
import de.cubeisland.engine.module.service.command.ContainerCommand;
import de.cubeisland.engine.module.service.command.CommandSender;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.core.util.converter.DurationConverter;
import de.cubeisland.engine.module.holiday.storage.HolidayModel;
import org.jooq.DSLContext;

import static de.cubeisland.engine.butler.parameter.Parameter.INFINITE;
import de.cubeisland.engine.module.service.user.TableUser.TABLE_USER;
import static de.cubeisland.engine.module.holiday.storage.TableHoliday.TABLE_HOLIDAY;

@Command(name = "holiday", desc = "Manages your holiday ")
public class HolidayCommands extends ContainerCommand
{
    private final DurationConverter converter = new DurationConverter();
    private final DSLContext dsl;

    public HolidayCommands(Holiday module)
    {
        super(module);
        dsl = module.getCore().getDB().getDSL();
    }

    @Command(name = "for", desc = "Starts your holiday and kicks you from the server")
    @Restricted(User.class)
    public void forCommand(User context, String duration, @Optional @Greed(INFINITE) String reason)
    {
        try
        {
            Date toDate = new Date(System.currentTimeMillis() + converter.fromNode(StringNode.of(duration)).getMillis());
            HolidayModel model = dsl.selectFrom(TABLE_HOLIDAY).where(TABLE_HOLIDAY.USERID.eq(context.getEntity().getId())).fetchOne();
            if (model == null)
            {
                model = dsl.newRecord(TABLE_HOLIDAY);
                model.setValue(TABLE_HOLIDAY.USERID, context.getEntity().getId());
            }
            model.setValue(TABLE_HOLIDAY.FROM, new Date(System.currentTimeMillis()));
            model.setValue(TABLE_HOLIDAY.TO, toDate);
            model.setValue(TABLE_HOLIDAY.REASON, reason);
            model.store();

            context.getEntity().setValue(TABLE_USER.NOGC, false);
            context.getEntity().updateAsync();

            context.kick(context.getTranslation(NEUTRAL, "You are now on holiday. See you later!"));
        }
        catch (ConversionException e)
        {
            context.sendTranslated(NEGATIVE, "Invalid duration format!");
        }
    }

    @Command(desc = "Checks the holiday status of a player")
    public void check(CommandSender context, User player)
    {
        HolidayModel model = dsl.selectFrom(TABLE_HOLIDAY).where(TABLE_HOLIDAY.USERID.eq(
            player.getEntity().getId())).fetchOne();
        if (model == null)
        {
            context.sendTranslated(NEUTRAL, "{user} is not on holiday!", player);
            return;
        }
        DateFormat df = DateFormat.getDateInstance(DateFormat.DEFAULT, context.getLocale());
        String dateTo = df.format(model.getValue(TABLE_HOLIDAY.TO));
        String dateFrom = df.format(model.getValue(TABLE_HOLIDAY.FROM));
        if (model.getValue(TABLE_HOLIDAY.TO).getTime() >= System.currentTimeMillis())
        {
            context.sendTranslated(POSITIVE, "{user} is on holiday from {input#date} to {input#date}", player, dateFrom, dateTo);
        }
        else
        {
            context.sendTranslated(POSITIVE, "{user} was on holiday from {input#date} to {input#date}", player, dateFrom, dateTo);
        }
        String reason = model.getValue(TABLE_HOLIDAY.REASON);
        if (reason != null)
        {
            context.sendTranslated(POSITIVE, "Reason: {input}", reason);
        }
    }
}
