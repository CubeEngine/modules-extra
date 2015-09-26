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
package de.cubeisland.engine.module.namehistory;

import java.sql.Date;
import java.text.SimpleDateFormat;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.command.CommandSender;
import de.cubeisland.engine.module.core.module.Module;
import org.cubeengine.service.user.TableUser;
import org.cubeengine.service.user.User;
import de.cubeisland.engine.service.user.UserLoadedEvent;
import org.cubeengine.module.core.util.McUUID;
import org.cubeengine.module.core.util.McUUID.NameEntry;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jooq.DSLContext;
import org.jooq.ResultQuery;
import org.jooq.SelectSeekStep1;

import static de.cubeisland.engine.module.namehistory.TableNameHistory.TABLE_NAMEHISTORY;

public class Namehistory extends Module implements Listener
{
    @Override
    public void onEnable()
    {
        this.getCore().getEventManager().registerListener(this, this);
        this.getCore().getDB().registerTable(TableNameHistory.class);

        CommandManager cm = this.getCore().getCommandManager();
        cm.addCommands(cm, this, this);
    }

    @EventHandler
    public void onJoin(UserLoadedEvent event)
    {
        DSLContext dsl = getCore().getDB().getDSL();
        User user = event.getUser();
        SelectSeekStep1<NameHistoryEntry, Date> query = dsl.selectFrom(TABLE_NAMEHISTORY)
                   .where(TABLE_NAMEHISTORY.USERID.eq(user.getEntity().getId()))
                   .orderBy(TABLE_NAMEHISTORY.CHANGED_AT.desc());
        getCore().getDB().queryOne(query.limit(1)).thenAccept(entry -> {
            if (entry == null || entry.getValue(TABLE_NAMEHISTORY.CHANGED_AT).getTime() > user.getLastPlayed()
                  || !entry.getValue(TABLE_NAMEHISTORY.NAME).equals(user.getName()))
            {
                NameEntry[] nameHistory = McUUID.getNameHistory(user.getUniqueId());
                for (NameEntry nameEntry : nameHistory)
                {
                    dsl.insertInto(TABLE_NAMEHISTORY).values(user.getEntity().getId(), nameEntry.name, new Date(nameEntry.changedToAt))
                       .onDuplicateKeyIgnore().execute();
                }
                entry = query.limit(1).fetchOne();
            }
            if (entry == null)
            {
                getLog().warn("Could not get NameHistory for {}", user.getName());
                return;
            }

            if (entry.getValue(TABLE_NAMEHISTORY.CHANGED_AT).getTime() > user.getEntity().getValue(TableUser.TABLE_USER.LASTSEEN).getTime())
            {
                entry = query.limit(1, 1).fetchOne();
                getCore().getUserManager().broadcastMessage(POSITIVE, "{name} was renamed to {user}", entry.getValue(TABLE_NAMEHISTORY.NAME), user);
            }
        });
    }

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    @Command(desc = "Shows the namehistory of a player")
    public void namehistory(CommandSender context, @Default User player)
    {
        ResultQuery<NameHistoryEntry> query = getCore().getDB().getDSL().selectFrom(TABLE_NAMEHISTORY).where(
            TABLE_NAMEHISTORY.USERID.eq(player.getEntity().getId())).orderBy(TABLE_NAMEHISTORY.CHANGED_AT.desc());
        getCore().getDB().query(query).thenAccept(result -> {
            if (result.isEmpty())
            {
                context.sendTranslated(NEGATIVE, "No NameHistory available for {user}", player);
                return;
            }
            context.sendTranslated(POSITIVE, "The following names were known for {user}", player);
            for (NameHistoryEntry entry : result)
            {
                Date value = entry.getValue(TABLE_NAMEHISTORY.CHANGED_AT);
                context.sendTranslated(NEUTRAL," - {user} since {input}", entry.getValue(TABLE_NAMEHISTORY.NAME),
                    value.getTime() <= 0 ? context.getTranslation(NONE, "account creation") : sdf.format(value));
            }
        });
    }
}
