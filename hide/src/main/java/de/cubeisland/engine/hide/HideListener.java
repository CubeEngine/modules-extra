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
package de.cubeisland.engine.hide;

import java.util.Set;

import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.core.user.UserManager;
import de.cubeisland.engine.hide.event.FakePlayerJoinEvent;
import de.cubeisland.engine.hide.event.FakePlayerQuitEvent;

public class HideListener implements Listener
{
    private final Hide module;
    private final UserManager um;

    public HideListener(Hide module)
    {
        this.module = module;
        this.um = module.getCore().getUserManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event)
    {
        final User joined = this.um.getExactUser(event.getPlayer().getName());
        if (event instanceof FakePlayerJoinEvent)
        {
            User user;
            for (String playerName : this.module.getCanSeeHiddens())
            {
                user = um.getExactUser(playerName);
                if (joined != user)
                {
                    user.sendTranslated("&aPlayer &e%s&a is now visible", joined.getDisplayName());
                }
            }
            return;
        }

        for (String playerName : this.module.getHiddenPlayers())
        {
            um.getExactUser(playerName).sendMessage(event.getJoinMessage());
        }

        if (!this.module.getCanSeeHiddens().contains(joined.getName()))
        {
            for (String playerName : this.module.getHiddenPlayers())
            {
                joined.hidePlayer(um.getExactUser(playerName));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event)
    {
        User player = um.getExactUser(event.getPlayer().toString());
        if (event instanceof FakePlayerQuitEvent)
        {
            User user;
            for (String playerName : this.module.getCanSeeHiddens())
            {
                user = um.getExactUser(playerName);
                if (player != user)
                {
                    user.sendTranslated("&aPlayer &e%s&a is now hidden!", player.getName());
                }
            }
            return;
        }

        if (event.getQuitMessage() != null)
        {
            Set<String> canSeeHiddens = this.module.getCanSeeHiddens();
            Set<String> hiddenPlayers = this.module.getHiddenPlayers();
            if (canSeeHiddens.contains(player.getName()))
            {
                for (String playerName : canSeeHiddens)
                {
                    um.getExactUser(playerName).sendMessage(event.getQuitMessage());
                }
                event.setQuitMessage(null);
                hiddenPlayers.remove(player.getName());
            }
            else
            {
                for (String playerName : hiddenPlayers)
                {
                    um.getExactUser(playerName).sendMessage(event.getQuitMessage());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event)
    {

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPickupItem(PlayerPickupItemEvent event)
    {
        if (this.module.getCanSeeHiddens().contains(event.getPlayer().getName()))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChat(AsyncPlayerChatEvent event)
    {
        Player player = event.getPlayer();
        Set<Player> recipients = event.getRecipients();
        if (this.module.getCanSeeHiddens().contains(player.getName()))
        {
            recipients.add(player);
        }
        else
        {
            final Server server = player.getServer();
            for (String playerName : this.module.getHiddenPlayers())
            {
                recipients.add(server.getPlayer(playerName));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityTarget(EntityTargetEvent event)
    {
        final Entity entity = event.getEntity();
        if (entity instanceof Player)
        {
            if (this.module.getHiddenPlayers().contains(((Player)entity).getName()))
            {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDropItem(PlayerDropItemEvent event)
    {
        if (this.module.getHiddenPlayers().contains(event.getPlayer().getName()) && !HidePerm.DROP.isAuthorized(event.getPlayer()))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPressurePlate(PlayerInteractEvent event)
    {
        if (event.getAction() == Action.PHYSICAL && this.module.getHiddenPlayers().contains(event.getPlayer().getName()))
        {
            event.setCancelled(true);
        }
    }
}
