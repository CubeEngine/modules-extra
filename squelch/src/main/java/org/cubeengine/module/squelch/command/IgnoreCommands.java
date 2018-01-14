/*
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
package org.cubeengine.module.squelch.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import javax.inject.Inject;
import org.cubeengine.butler.filter.Restricted;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.module.squelch.Squelch;
import org.cubeengine.module.squelch.storage.IgnoreList;
import org.cubeengine.libcube.util.ChatFormat;
import org.cubeengine.libcube.util.StringUtils;
import org.cubeengine.module.sql.database.Database;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;

import static org.cubeengine.module.squelch.storage.TableIgnorelist.TABLE_IGNORE_LIST;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.*;

public class IgnoreCommands
{
    private final Squelch module;
    private Database db;
    private I18n i18n;

    private Map<UUID, List<IgnoreList>> ignored = new HashMap<>(); // TODO chacheing

    @Inject
    public IgnoreCommands(I18n i18n, Squelch module, Database db)
    {
        this.module = module;
        this.db = db;
        this.i18n = i18n;
    }

    private boolean addIgnore(Player user, User ignored)
    {
        if (checkIgnored(user, ignored))
        {
            return false;
        }

        IgnoreList ignoreList = db.getDSL().newRecord(TABLE_IGNORE_LIST).newIgnore(user.getUniqueId(), ignored.getUniqueId());
        ignoreList.insertAsync();
        return true;
    }

    private boolean removeIgnore(Player user, User ignored)
    {
        if (checkIgnored(user, ignored))
        {
            db.getDSL().delete(TABLE_IGNORE_LIST).
                where(TABLE_IGNORE_LIST.ID.eq(user.getUniqueId())).
                and(TABLE_IGNORE_LIST.IGNORE.eq(ignored.getUniqueId())).execute();
            return true;
        }
        return true;
    }

    public boolean checkIgnored(Player user, User ignored)
    {
        // TODO cache this shit
        IgnoreList ignore =
            db.getDSL().selectFrom(TABLE_IGNORE_LIST).
                where(TABLE_IGNORE_LIST.ID.eq(user.getUniqueId())).
                and(TABLE_IGNORE_LIST.IGNORE.eq(ignored.getUniqueId())).fetchOneInto(
                TABLE_IGNORE_LIST);
        return ignore != null;
    }

    @Command(desc = "Ignores all messages from players")
    public void ignore(CommandSource context, List<User> players)
    {
        if (!(context instanceof Player))
        {
            int rand1 = new Random().nextInt(6) + 1;
            int rand2 = new Random().nextInt(6 - rand1 + 1) + 1;
            i18n.send(context, NEUTRAL, "Ignore ({text:8+:color=WHITE}): {integer#random} + {integer#random} = {integer#sum} -> {text:failed:color=RED}",
                                   rand1, rand2, rand1 + rand2);
            return;
        }
        
        Player sender = ((Player)context);
        List<String> added = new ArrayList<>();
        for (User user : players)
        {
            if (user == context)
            {
                i18n.send(context, NEGATIVE, "If you do not feel like talking to yourself just don't talk.");
            }
            else if (!this.addIgnore(sender, user))
            {
                if (user.hasPermission(module.perms().COMMAND_IGNORE_PREVENT.getId()))
                {
                    i18n.send(context, NEGATIVE, "You are not allowed to ignore {user}!", user);
                    continue;
                }
                i18n.send(context, NEGATIVE, "{user} is already on your ignore list!", user);
            }
            else
            {
                added.add(user.getName());
            }
        }
        if (added.isEmpty())
        {
            return;
        }
        i18n.send(context, POSITIVE, "You added {user#list} to your ignore list!", StringUtils.implode(ChatFormat.WHITE + ", " + ChatFormat.DARK_GREEN, added));
    }

    @Command(desc = "Stops ignoring all messages from a player")
    @Restricted(value = Player.class, msg = "Congratulations! You are now looking at this text!")
    public void unignore(Player context, List<User> players)
    {
        List<String> added = new ArrayList<>();
        for (User user : players)
        {
            if (!this.removeIgnore(context, user))
            {
                i18n.send(context, NEGATIVE, "You haven't ignored {user}!", user);
            }
            else
            {
                added.add(user.getName());
            }
        }
        if (added.isEmpty())
        {
            return;
        }
        i18n.send(context, POSITIVE, "You removed {user#list} from your ignore list!", StringUtils.implode(ChatFormat.WHITE + ", " + ChatFormat.DARK_GREEN, added));
    }
}
