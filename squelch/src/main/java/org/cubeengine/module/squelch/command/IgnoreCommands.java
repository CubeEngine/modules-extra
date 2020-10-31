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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Restricted;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.util.ChatFormat;
import org.cubeengine.libcube.util.StringUtils;
import org.cubeengine.module.squelch.SquelchPerm;
import org.cubeengine.module.squelch.data.SquelchData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

@Singleton
public class IgnoreCommands
{
    private I18n i18n;
    private SquelchPerm perms;

    @Inject
    public IgnoreCommands(I18n i18n, SquelchPerm perms)
    {
        this.i18n = i18n;
        this.perms = perms;
    }

    private boolean addIgnore(ServerPlayer player, UUID ignored)
    {
        if (checkIgnored(player, ignored))
        {
            return false;
        }

        final List<UUID> ignoreList = player.get(SquelchData.IGNORED).orElse(new ArrayList<>());
        ignoreList.add(ignored);
        player.offer(SquelchData.IGNORED, ignoreList);
        return true;
    }

    private boolean removeIgnore(ServerPlayer player, UUID ignored)
    {
        final List<UUID> ignoreList = player.get(SquelchData.IGNORED).orElse(new ArrayList<>());
        final boolean removed = ignoreList.remove(ignored);
        if (removed)
        {
            player.offer(SquelchData.IGNORED, ignoreList);
        }
        return removed;
    }

    public boolean checkIgnored(Player player, UUID check)
    {
        return player.get(SquelchData.IGNORED).map(list -> list.contains(check)).orElse(false);
    }

    @Command(desc = "Ignores all messages from players")
    public void ignore(ServerPlayer sender, User player) // TODO User list
    {
        List<String> added = new ArrayList<>();
        for (User user : Arrays.asList(player))
        {
            if (user.getUniqueId().equals(sender.getUniqueId()))
            {
                i18n.send(sender, NEGATIVE, "If you do not feel like talking to yourself just don't talk.");
            }
            else if (!this.addIgnore(sender, user.getUniqueId()))
            {
                if (user.hasPermission(perms.COMMAND_IGNORE_PREVENT.getId()))
                {
                    i18n.send(sender, NEGATIVE, "You are not allowed to ignore {user}!", user);
                    continue;
                }
                i18n.send(sender, NEGATIVE, "{user} is already on your ignore list!", user);
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
        i18n.send(sender, POSITIVE, "You added {user#list} to your ignore list!",
                  StringUtils.implode(ChatFormat.WHITE + ", " + ChatFormat.DARK_GREEN, added));
    }

    @Command(desc = "Stops ignoring all messages from a player")
    @Restricted(msg = "Congratulations! You are now looking at this text!")
    public void unignore(ServerPlayer context, User player) // TODO User list
    {
        List<String> added = new ArrayList<>();
        for (User user : Arrays.asList(player))
        {
            if (!this.removeIgnore(context, user.getUniqueId()))
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
        i18n.send(context, POSITIVE, "You removed {user#list} from your ignore list!",
                  StringUtils.implode(ChatFormat.WHITE + ", " + ChatFormat.DARK_GREEN, added));
    }
}
