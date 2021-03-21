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
package org.cubeengine.module.chat.command;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.Broadcaster;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.cubeengine.libcube.service.command.annotation.Default;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.module.chat.ChatConfig;
import org.cubeengine.module.chat.ChatPerm;
import org.cubeengine.module.chat.listener.AfkListener;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

@Singleton
public class AfkCommand
{
    private long autoAfk;
    private long afkCheck;
    private final AfkListener listener;
    private Broadcaster bc;
    private TaskManager tm;
    private EventManager em;
    private I18n i18n;
    private ChatPerm perms;

    private Map<UUID, Boolean> afks = new HashMap<>();
    private Map<UUID, Long> actions = new HashMap<>();

    @Inject
    public AfkCommand(Broadcaster bc, TaskManager tm, EventManager em, I18n i18n, ChatPerm perms)
    {
        this.listener = new AfkListener(this, perms);
        this.bc = bc;
        this.tm = tm;
        this.em = em;
        this.i18n = i18n;
        this.perms = perms;
    }

    public void init(ChatConfig config)
    {
        this.autoAfk = config.autoAfk.after.toMillis();
        this.afkCheck = config.autoAfk.check.toMillis();
        if (afkCheck > 0)
        {
            em.registerListener(listener);
            if (autoAfk > 0)
            {
                tm.runTimer(t -> checkAfk(), Duration.ofSeconds(1), config.autoAfk.check);
            }
        }
    }

    @Command(desc = "Displays that you are afk")
    public void afk(CommandCause context, @Default ServerPlayer player)
    {
        if (!context.audience().equals(player))
        {
            if (!perms.COMMAND_AFK_OTHER.check(context.subject(), context.audience(), i18n))
            {
                return;
            }
        }
        if (afks.getOrDefault(player.uniqueId(), false))
        {
            updateLastAction(player);
            checkAfk();
            return;
        }
        setAfk(player, true);
        resetLastAction(player);
        bc.broadcastStatus("is now afk.", player);
    }


    public boolean isAfk(Player player)
    {
        return afks.getOrDefault(player.uniqueId(), false);
    }

    public void setAfk(Player player, boolean afk)
    {
        afks.put(player.uniqueId(), afk);
    }

    public long getLastAction(Player player)
    {
        return actions.getOrDefault(player.uniqueId(), 0L);
    }

    public long updateLastAction(Player player)
    {
        actions.put(player.uniqueId(), System.currentTimeMillis());
        return getLastAction(player);
    }

    public void resetLastAction(Player player)
    {
        actions.remove(player.uniqueId());
    }

    public void checkAfk()
    {
        afks.entrySet().stream().filter(Entry::getValue)
            .map(Entry::getKey).map(uuid -> Sponge.server().userManager().find(uuid))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(this::updateAfk);
    }

    private void updateAfk(User user)
    {
        if (user.player().isPresent())
        {
            ServerPlayer player = user.player().get();
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

                if (!perms.PREVENT_AUTOAFK.check(player))
                {
                    setAfk(player, true);
                    this.bc.broadcastStatus("is now afk!", player);
                }
            }
            return;
        }
        afks.remove(user.uniqueId());
        actions.remove(user.uniqueId());
    }
}
