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

import de.cubeisland.engine.module.core.module.Module;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.holiday.storage.HolidayModel;
import de.cubeisland.engine.module.holiday.storage.TableHoliday;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jooq.DSLContext;

import de.cubeisland.engine.module.service.user.TableUser.TABLE_USER;
import de.cubeisland.engine.module.core.util.formatter.MessageType.POSITIVE;
import static de.cubeisland.engine.module.holiday.storage.TableHoliday.TABLE_HOLIDAY;

public class Holiday extends Module implements Listener
{
    private DSLContext dsl;

    @Override
    public void onEnable()
    {
        this.getCore().getDB().registerTable(TableHoliday.class);
        this.getCore().getCommandManager().addCommand(new HolidayCommands(this));
        dsl = this.getCore().getDB().getDSL();
        this.getCore().getEventManager().registerListener(this, this);
        this.getCore().getTaskManager().runTimer(this, new Runnable()
        {
            @Override
            public void run()
            {
                checkExpiredHolidays();
            }
        }, 0, 20 * 60 * 60 * 24);
    }

    private void checkExpiredHolidays()
    {
        long current = System.currentTimeMillis();
        for (HolidayModel model : this.dsl.selectFrom(TABLE_HOLIDAY).fetch())
        {
            if (model.getValue(TABLE_HOLIDAY.TO).getTime() < current)
            {
                User user = this.getCore().getUserManager().getUser(model.getValue(TABLE_HOLIDAY.USERID));
                user.getEntity().setValue(TABLE_USER.NOGC, false);
                user.getEntity().updateAsync();
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event)
    {
        User user = this.getCore().getUserManager().getExactUser(event.getPlayer().getUniqueId());
        HolidayModel existing = dsl.selectFrom(TABLE_HOLIDAY).where(TABLE_HOLIDAY.USERID.eq(user.getEntity().getKey())).fetchOne();
        if (existing != null)
        {
            existing.deleteAsync();
            user.sendTranslated(POSITIVE, "Welcome back!");
            user.getEntity().setValue(TABLE_USER.NOGC, false);
            user.getEntity().updateAsync();
        }
    }
}
