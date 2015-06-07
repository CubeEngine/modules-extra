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
package de.cubeisland.engine.module.chat.listener;

import de.cubeisland.engine.module.chat.Chat;
import de.cubeisland.engine.module.chat.ChatAttachment;
import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.service.user.UserManager;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.entity.ProjectileLaunchEvent;
import org.spongepowered.api.event.entity.player.PlayerChatEvent;
import org.spongepowered.api.event.entity.player.PlayerInteractEvent;
import org.spongepowered.api.event.entity.player.PlayerMoveEvent;
import org.spongepowered.api.event.entity.player.PlayerQuitEvent;
import org.spongepowered.api.event.inventory.InventoryClickEvent;
import org.spongepowered.api.event.message.CommandEvent;

import static org.spongepowered.api.event.Order.POST;

public class AfkListener implements Runnable
{
    private final Chat module;
    private final UserManager um;
    private final long autoAfk;
    private final long afkCheck;

    public AfkListener(Chat module, UserManager um, long autoAfk, long afkCheck)
    {
        this.module = module;
        this.um = um;
        this.autoAfk = autoAfk;
        this.afkCheck = afkCheck;
    }
    @Subscribe(order = POST)
    public void onMove(PlayerMoveEvent event)
    {
        if (event.getOldLocation().getBlockX() == event.getNewLocation().getBlockX() && event.getOldLocation().getBlockZ() == event.getNewLocation().getBlockZ())
        {
            return;
        }
        this.updateLastAction(event.getUser());
    }

    @Subscribe(order = POST)
    public void onInventoryClick(InventoryClickEvent event)
    {
        if (event.getViewer() instanceof Player)
        {
            this.updateLastAction((Player)event.getViewer());
        }
    }

    @Subscribe(order = POST)
    public void playerInteract(PlayerInteractEvent event)
    {
        this.updateLastAction(event.getUser());
    }

    @Subscribe(order = POST)
    public void onChat(PlayerChatEvent event)
    {
        this.updateLastAction(event.getUser());
        this.run();
    }

    @Subscribe(order = POST)
    public void onCommand(CommandEvent event)
    {
        if (event.getSource() instanceof Player)
        {
            this.updateLastAction((Player)event.getSource());
        }
    }

    /* TODO @Subscribe(order = POST)
    public void onChatTabComplete(PlayerChatTabCompleteEvent event)
    {
        this.updateLastAction(event.getUser());
    }
    */

    @Subscribe(order = POST)
    public void onLeave(PlayerQuitEvent event)
    {
        ChatAttachment attachment = this.um.getExactUser(event.getUser().getUniqueId()).get(ChatAttachment.class);
        if (attachment != null)
        {
            attachment.setAfk(false);
            attachment.resetLastAction();
        }
    }

    @Subscribe(order = POST)
    public void onBowShot(ProjectileLaunchEvent event)
    {
        if (event.getSource().isPresent() && event.getSource() instanceof Player)
        {
            this.updateLastAction((Player)event.getEntity());
        }
    }

    private void updateLastAction(Player player)
    {
        ChatAttachment basicsAttachment = this.um.getExactUser(player.getUniqueId()).get(ChatAttachment.class);
        if (basicsAttachment != null)
        {
            if (basicsAttachment.isAfk() && module.perms().PREVENT_AUTOUNAFK.isAuthorized(player))
            {
                return;
            }
            basicsAttachment.updateLastAction();
        }
    }

    @Override
    public void run()
    {
        um.getLoadedUsers().stream()
          .filter(User::isOnline)
          .map(u -> u.attachOrGet(ChatAttachment.class, this.module))
          .forEach(this::updateAfk);
    }

    private void updateAfk(ChatAttachment attachment)
    {
        long lastAction = attachment.getLastAction();
        if (lastAction == 0)
        {
            return;
        }
        if (attachment.isAfk())
        {
            if (System.currentTimeMillis() - lastAction < this.afkCheck)
            {
                attachment.setAfk(false);
                this.um.broadcastStatus("is no longer afk!", attachment.getHolder());
            }
        }
        else if (System.currentTimeMillis() - lastAction > this.autoAfk)
        {
            if (!module.perms().PREVENT_AUTOAFK.isAuthorized(attachment.getHolder()))
            {
                attachment.setAfk(true);
                this.um.broadcastStatus("is now afk!" ,attachment.getHolder());
            }
        }
    }
}
