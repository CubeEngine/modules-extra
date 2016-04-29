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

import java.util.UUID;
import javax.inject.Inject;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.cubeengine.module.holiday.storage.HolidayModel;
import org.cubeengine.module.holiday.storage.TableHoliday;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.database.Database;
import org.cubeengine.libcube.service.database.ModuleTables;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.task.TaskManager;
import org.jooq.DSLContext;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ClientConnectionEvent;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.module.holiday.storage.TableHoliday.TABLE_HOLIDAY;


@ModuleInfo(name = "Holiday", description = "Marks you as being on holiday")
public class Holiday extends Module
{
    private DSLContext dsl;
    @Inject private Database db;
    @Inject private CommandManager cm;
    @Inject private TaskManager tm;
    @Inject private EventManager em;
    @Inject private I18n i18n;

    @Enable
    public void onEnable()
    {
        dsl = db.getDSL();
        cm.addCommand(new HolidayCommands(this, dsl, i18n));
        em.registerListener(this, this);
        tm.runTimer(this, this::checkExpiredHolidays, 0, 20 * 60 * 60 * 24);
    }

    private void checkExpiredHolidays()
    {
        long current = System.currentTimeMillis();
        for (HolidayModel model : this.dsl.selectFrom(TABLE_HOLIDAY).fetch())
        {
            if (model.getValue(TABLE_HOLIDAY.TO).getTime() < current)
            {
                UUID userUUID = model.getValue(TABLE_HOLIDAY.USERID);
                // TODO no gc no longer exists due to removed user table
            }
        }
    }

    @Listener
    public void onJoin(ClientConnectionEvent.Join event)
    {
        Player player = event.getTargetEntity();
        HolidayModel existing = dsl.selectFrom(TABLE_HOLIDAY).where(TABLE_HOLIDAY.USERID.eq(
            player.getUniqueId())).fetchOne();
        if (existing != null)
        {
            existing.deleteAsync();
            i18n.sendTranslated(player, POSITIVE, "Welcome back!");
        }
    }
}
