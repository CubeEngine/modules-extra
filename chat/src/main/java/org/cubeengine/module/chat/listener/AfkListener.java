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
package org.cubeengine.module.chat.listener;

import org.cubeengine.module.chat.Chat;
import org.cubeengine.module.chat.ChatAttachment;
import org.cubeengine.service.user.UserManager;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.entity.DisplaceEntityEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.living.player.PlayerChatEvent;
import org.spongepowered.api.event.entity.living.player.PlayerQuitEvent;
import org.spongepowered.api.event.entity.projectile.LaunchProjectileEvent;
import org.spongepowered.api.event.inventory.InventoryClickEvent;

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
    @Listener(order = POST)
    public void onMove(DisplaceEntityEvent.Move.TargetPlayer event)
    {
        if (event.getOldTransform().getLocation().getBlockX() == event.getNewTransform().getLocation().getBlockX()
            && event.getOldTransform().getLocation().getBlockZ() == event.getNewTransform().getLocation().getBlockZ())
        {
            return;
        }
        this.updateLastAction(event.getTargetEntity());
    }

    @Listener(order = POST)
    public void onInventoryClick(InventoryClickEvent event)
    {
        if (event.getViewer() instanceof Player)
        {
            this.updateLastAction((Player)event.getViewer());
        }
    }

    @Listener(order = POST)
    public void playerInteract(InteractBlockEvent.SourcePlayer event)
    {
        this.updateLastAction(event.getSourceEntity());
    }


    @Listener(order = POST)
    public void playerInteract(InteractEntityEvent.SourcePlayer event)
    {
        this.updateLastAction(event.getSourceEntity());
    }

    @Listener(order = POST)
    public void onChat(PlayerChatEvent event)
    {
        this.updateLastAction(event.getSource());
        this.run();
    }

    @Listener(order = POST)
    public void onCommand(SendCommandEvent event)
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

    @Listener(order = POST)
    public void onLeave(PlayerQuitEvent event)
    {
        ChatAttachment attachment = this.um.getExactUser(event.getSource().getUniqueId()).get(ChatAttachment.class);
        if (attachment != null)
        {
            attachment.setAfk(false);
            attachment.resetLastAction();
        }
    }

    @Listener(order = POST)
    public void onBowShot(LaunchProjectileEvent.SourcePlayer event)
    {
        this.updateLastAction(event.getSourceEntity());
    }

    private void updateLastAction(Player player)
    {
        ChatAttachment basicsAttachment = this.um.getExactUser(player.getUniqueId()).get(ChatAttachment.class);
        if (basicsAttachment != null)
        {
            if (basicsAttachment.isAfk() && player.hasPermission(module.perms().PREVENT_AUTOUNAFK.getId()))
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
          .filter(u -> u.getPlayer().isPresent())
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
            if (!attachment.getHolder().hasPermission(module.perms().PREVENT_AUTOAFK.getId()))
            {
                attachment.setAfk(true);
                this.um.broadcastStatus("is now afk!" ,attachment.getHolder());
            }
        }
    }
}
