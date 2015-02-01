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
import de.cubeisland.engine.command.parametric.Command;
import de.cubeisland.engine.command.parametric.Default;
import de.cubeisland.engine.core.CubeEngine;
import de.cubeisland.engine.core.command.CommandManager;
import de.cubeisland.engine.core.command.CommandSender;
import de.cubeisland.engine.core.module.Module;
import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.user.UserLoadedEvent;
import de.cubeisland.engine.core.util.McUUID;
import de.cubeisland.engine.core.util.McUUID.NameEntry;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jooq.DSLContext;
import org.jooq.ResultQuery;
import org.jooq.SelectOffsetStep;

import static de.cubeisland.engine.core.util.formatter.MessageType.*;
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
        SelectOffsetStep<NameHistoryEntry> query = dsl.selectFrom(TABLE_NAMEHISTORY).
            where(TABLE_NAMEHISTORY.USERID.eq(user.getEntity().getKey())).
            orderBy(TABLE_NAMEHISTORY.CHANGED_AT.desc()).limit(1);
        getCore().getDB().queryOne(query).thenAccept(entry -> {
            if (entry == null || entry.getValue(TABLE_NAMEHISTORY.CHANGED_AT).getTime() > user.getLastPlayed())
            {
                NameEntry[] nameHistory = McUUID.getNameHistory(user.getUniqueId());
                for (NameEntry nameEntry : nameHistory)
                {
                    dsl.insertInto(TABLE_NAMEHISTORY).values(user.getEntity().getKey(), nameEntry.name, new Date(
                        nameEntry.changedToAt)).onDuplicateKeyIgnore().execute();
                }
                entry = query.fetchOne();
            }
            if (entry == null)
            {
                getLog().warn("Could not get NameHistory for {}", user.getName());
                return;
            }

            if (entry.getValue(TABLE_NAMEHISTORY.CHANGED_AT).getTime() > user.getLastPlayed())
            {
                getCore().getUserManager().broadcastMessage(POSITIVE, "{name} was renamed to {user}", entry.getValue(
                    TABLE_NAMEHISTORY.NAME), user);
            }
        });
    }

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    @Command(desc = "Shows the namehistory of a player")
    public void nameHistory(CommandSender context, @Default User player)
    {
        ResultQuery<NameHistoryEntry> query = getCore().getDB().getDSL().selectFrom(TABLE_NAMEHISTORY).where(
            TABLE_NAMEHISTORY.USERID.eq(player.getEntity().getKey())).orderBy(TABLE_NAMEHISTORY.CHANGED_AT.desc());
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