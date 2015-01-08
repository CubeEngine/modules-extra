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

import de.cubeisland.engine.command.filter.Restricted;
import de.cubeisland.engine.command.methodic.Command;
import de.cubeisland.engine.command.methodic.Param;
import de.cubeisland.engine.command.methodic.Params;
import de.cubeisland.engine.command.methodic.parametric.Greed;
import de.cubeisland.engine.command.methodic.parametric.Label;
import de.cubeisland.engine.command.methodic.parametric.Optional;
import de.cubeisland.engine.converter.ConversionException;
import de.cubeisland.engine.converter.node.StringNode;
import de.cubeisland.engine.core.command.CommandContainer;
import de.cubeisland.engine.core.command.CommandContext;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.util.converter.DurationConverter;
import de.cubeisland.engine.module.holiday.storage.HolidayModel;
import org.jooq.DSLContext;

import static de.cubeisland.engine.command.parameter.Parameter.INFINITE;
import static de.cubeisland.engine.core.user.TableUser.TABLE_USER;
import static de.cubeisland.engine.core.util.formatter.MessageType.*;
import static de.cubeisland.engine.module.holiday.storage.TableHoliday.TABLE_HOLIDAY;

@Command(name = "holiday", desc = "Manages your holiday ")
public class HolidayCommands extends CommandContainer
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
    public void forCommand(CommandContext context, @Label("duration") String duration, @Optional @Greed(INFINITE) @Label("reason") String reason)
    {
        try
        {
            Date toDate = new Date(System.currentTimeMillis() + converter.fromNode(StringNode.of(duration)).getMillis());
            User sender = (User)context.getSource();
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
            sender.getEntity().updateAsync();

            sender.kickPlayer(sender.getTranslation(NEUTRAL, "You are now on holiday. See you later!"));
        }
        catch (ConversionException e)
        {
            context.sendTranslated(NEGATIVE, "Invalid duration format!");
        }
    }

    @Command(desc = "Checks the holiday status of a player")
    @Params(positional = @Param(label = "player", type = User.class))
    public void check(CommandContext context, @Label("player") User user)
    {
        HolidayModel model = dsl.selectFrom(TABLE_HOLIDAY).where(TABLE_HOLIDAY.USERID.eq(user.getEntity().getKey())).fetchOne();
        if (model == null)
        {
            context.sendTranslated(NEUTRAL, "{user} is not on holiday!", user);
            return;
        }
        DateFormat df = DateFormat.getDateInstance(DateFormat.DEFAULT, context.getSource().getLocale());
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
