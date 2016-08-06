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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import org.cubeengine.butler.parametric.Command;
import org.cubeengine.butler.parametric.Default;
import org.cubeengine.module.chat.Chat;
import org.cubeengine.module.chat.listener.AfkListener;
import org.cubeengine.libcube.service.command.exception.PermissionDeniedException;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.libcube.service.Broadcaster;
import org.spongepowered.api.Game;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;

public class AfkCommand implements Runnable
{
    private final Chat module;
    private long autoAfk;
    private long afkCheck;
    private Game game;
    private final AfkListener listener;
    private Broadcaster bc;

    private Map<UUID, Boolean> afks = new HashMap<>();
    private Map<UUID, Long> actions = new HashMap<>();

    public AfkCommand(Chat module, long autoAfk, long afkCheck, Broadcaster bc, TaskManager tm, EventManager em, Game game)
    {
        this.module = module;
        this.autoAfk = autoAfk;
        this.afkCheck = afkCheck;
        this.game = game;
        this.listener = new AfkListener(module, this);
        if (afkCheck > 0)
        {
            em.registerListener(Chat.class, listener);
            if (autoAfk > 0)
            {
                tm.runTimer(Chat.class, this, 20, afkCheck / 50); // this is in ticks so /50
            }
        }
        this.bc = bc;
    }

    @Command(desc = "Displays that you are afk")
    public void afk(CommandSource context, @Default Player player)
    {
        if (!context.equals(player))
        {
            if (context.hasPermission(module.perms().COMMAND_AFK_OTHER.getId()))
            {
                throw new PermissionDeniedException(module.perms().COMMAND_AFK_OTHER);
            }
        }
        if (afks.getOrDefault(player.getUniqueId(), false))
        {
            updateLastAction(player);
            this.run();
            return;
        }
        setAfk(player, true);
        resetLastAction(player);
        bc.broadcastStatus("is now afk.", player);
    }


    public boolean isAfk(Player player)
    {
        return afks.getOrDefault(player.getUniqueId(), false);
    }

    public void setAfk(Player player, boolean afk)
    {
        afks.put(player.getUniqueId(), afk);
    }

    public long getLastAction(Player player)
    {
        return actions.getOrDefault(player.getUniqueId(), 0L);
    }

    public long updateLastAction(Player player)
    {
        actions.put(player.getUniqueId(), System.currentTimeMillis());
        return getLastAction(player);
    }

    public void resetLastAction(Player player)
    {
        actions.remove(player.getUniqueId());
    }

    @Override
    public void run()
    {
        afks.entrySet().stream().filter(Entry::getValue)
            .map(Entry::getKey).map(uuid -> game.getServiceManager().provide(UserStorageService.class).get().get(uuid))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(this::updateAfk);
    }

    private void updateAfk(User user)
    {
        if (user.getPlayer().isPresent())
        {
            Player player = user.getPlayer().get();
            long lastAction = getLastAction(player);
            if (lastAction == 0)
            {
                return;
            }
            if (isAfk(player))
            {
                if (System.currentTimeMillis() - lastAction < this.afkCheck)
                {
                    setAfk(player, false);
                    this.bc.broadcastStatus("is no longer afk!", player);
                }
            }
            else if (System.currentTimeMillis() - lastAction > this.autoAfk)
            {
                if (!player.hasPermission(module.perms().PREVENT_AUTOAFK.getId()))
                {
                    setAfk(player, true);
                    this.bc.broadcastStatus("is now afk!", player);
                }
            }
            return;
        }
        afks.remove(user.getUniqueId());
        actions.remove(user.getUniqueId());
    }
}
