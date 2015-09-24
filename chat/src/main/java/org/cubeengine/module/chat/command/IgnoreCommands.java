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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import de.cubeisland.engine.butler.filter.Restricted;
import de.cubeisland.engine.butler.parametric.Command;
import de.cubeisland.engine.butler.parametric.Reader;
import org.cubeengine.module.chat.Chat;
import org.cubeengine.module.chat.storage.IgnoreList;
import org.cubeengine.module.chat.storage.TableIgnorelist;
import org.cubeengine.module.core.util.ChatFormat;
import org.cubeengine.module.core.util.StringUtils;
import org.cubeengine.service.command.CommandContext;
import org.cubeengine.service.database.Database;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.user.UserManager;
import org.spongepowered.api.entity.living.player.Player;

import static org.cubeengine.service.i18n.formatter.MessageType.*;

public class IgnoreCommands
{
    private final Chat module;
    private Database db;
    private UserManager um;
    private I18n i18n;

    private Map<UUID, List<IgnoreList>> ignored = new HashMap<>();

    public IgnoreCommands(Chat basics, Database db, UserManager um)
    {
        this.module = basics;
        this.db = db;
        this.um = um;
    }

    private boolean addIgnore(Player user, Player ignored)
    {
        if (checkIgnored(user, ignored))
        {
            return false;
        }

        IgnoreList ignoreList = db.getDSL().newRecord(TableIgnorelist.TABLE_IGNORE_LIST)
              .newIgnore(um.getByUUID(user.getUniqueId()).getEntity().getId(),
                         um.getByUUID(ignored.getUniqueId()).getEntity().getId());
        ignoreList.insertAsync();
        return true;
    }

    private boolean removeIgnore(Player user, Player ignored)
    {
        if (checkIgnored(user, ignored))
        {
            db.getDSL().delete(TableIgnorelist.TABLE_IGNORE_LIST).
                where(TableIgnorelist.TABLE_IGNORE_LIST.ID.eq(um.getByUUID(user.getUniqueId()).getEntity().getId())).
                and(TableIgnorelist.TABLE_IGNORE_LIST.IGNORE.eq(um.getByUUID(ignored.getUniqueId()).getEntity().getId())).execute();
            return true;
        }
        return true;
    }

    public boolean checkIgnored(Player user, Player ignored)
    {
        // TODO cache this shit
        IgnoreList ignore =
            db.getDSL().selectFrom(TableIgnorelist.TABLE_IGNORE_LIST).
                where(TableIgnorelist.TABLE_IGNORE_LIST.ID.eq(um.getByUUID(user.getUniqueId()).getEntity().getId())).
                and(TableIgnorelist.TABLE_IGNORE_LIST.IGNORE.eq(um.getByUUID(ignored.getUniqueId()).getEntity().getId())).fetchOneInto(
                TableIgnorelist.TABLE_IGNORE_LIST);
        return ignore != null;
    }

    @Command(desc = "Ignores all messages from players")
    public void ignore(CommandContext context, @Reader(Player.class) List<Player> players)
    {
        if (!context.isSource(Player.class))
        {
            int rand1 = new Random().nextInt(6) + 1;
            int rand2 = new Random().nextInt(6 - rand1 + 1) + 1;
            context.sendTranslated(NEUTRAL, "Ignore ({text:8+:color=WHITE}): {integer#random} + {integer#random} = {integer#sum} -> {text:failed:color=RED}",
                                   rand1, rand2, rand1 + rand2);
            return;
        }
        Player sender = ((Player)context.getSource());
        List<String> added = new ArrayList<>();
        for (Player user : players)
        {
            if (user == context.getSource())
            {
                context.sendTranslated(NEGATIVE, "If you do not feel like talking to yourself just don't talk.");
            }
            else if (!this.addIgnore(sender, user))
            {
                if (user.hasPermission(module.perms().COMMAND_IGNORE_PREVENT.getId()))
                {
                    context.sendTranslated(NEGATIVE, "You are not allowed to ignore {user}!", user);
                    continue;
                }
                context.sendTranslated(NEGATIVE, "{user} is already on your ignore list!", user);
            }
            else
            {
                added.add(user.getName());
            }
        }
        context.sendTranslated(POSITIVE, "You added {user#list} to your ignore list!", StringUtils.implode(ChatFormat.WHITE + ", " + ChatFormat.DARK_GREEN, added));
    }

    @Command(desc = "Stops ignoring all messages from a player")
    @Restricted(value = Player.class, msg = "Congratulations! You are now looking at this text!")
    public void unignore(Player context, @Reader(Player.class) List<Player> players)
    {
        List<String> added = new ArrayList<>();
        for (Player user : players)
        {
            if (!this.removeIgnore(context, user))
            {
                i18n.sendTranslated(context, NEGATIVE, "You haven't ignored {user}!", user);
            }
            else
            {
                added.add(user.getName());
            }
        }
        i18n.sendTranslated(context, POSITIVE, "You removed {user#list} from your ignore list!", StringUtils.implode(ChatFormat.WHITE + ", " + ChatFormat.DARK_GREEN, added));
    }
}
