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
import org.cubeengine.module.chat.command.AfkCommand;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.command.TabCompleteEvent;
import org.spongepowered.api.event.entity.DisplaceEntityEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.projectile.LaunchProjectileEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.item.inventory.InteractInventoryEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;

import static org.spongepowered.api.event.Order.POST;

public class AfkListener
{
    private final Chat module;
    private AfkCommand afkCommand;

    public AfkListener(Chat module, AfkCommand afkCommand)
    {
        this.module = module;
        this.afkCommand = afkCommand;
    }

    @Listener(order = POST)
    public void onMove(DisplaceEntityEvent.Move.TargetPlayer event)
    {
        if (event.getFromTransform().getLocation().getBlockX() == event.getToTransform().getLocation().getBlockX()
            && event.getFromTransform().getLocation().getBlockZ() == event.getToTransform().getLocation().getBlockZ())
        {
            return;
        }
        this.updateLastAction(event.getTargetEntity());
    }

    @Listener(order = POST)
    public void onInventoryInteract(InteractInventoryEvent event, @First Player player)
    {
        this.updateLastAction(player);
    }

    @Listener(order = POST)
    public void playerInteract(InteractBlockEvent event, @First Player player)
    {
        this.updateLastAction(player);
    }


    @Listener(order = POST)
    public void playerInteract(InteractEntityEvent event, @First Player player)
    {
        this.updateLastAction(player);
    }

    @Listener(order = POST)
    public void onChat(MessageChannelEvent event, @First Player player)
    {
        this.updateLastAction(player);
        afkCommand.run();
    }

    @Listener(order = POST)
    public void onCommand(SendCommandEvent event, @First Player player)
    {
        this.updateLastAction(player);
    }

    @Listener(order = POST)
    public void onTabComplete(TabCompleteEvent event, @First Player player)
    {
        this.updateLastAction(player);
    }

    @Listener(order = POST)
    public void onLeave(ClientConnectionEvent.Disconnect event, @First Player player)
    {
        afkCommand.setAfk(player, false);
        afkCommand.resetLastAction(player);
    }

    @Listener(order = POST)
    public void onBowShot(LaunchProjectileEvent event, @First Player source)
    {
        this.updateLastAction(source);
    }

    private void updateLastAction(Player player)
    {
        if (afkCommand.isAfk(player) && player.hasPermission(module.perms().PREVENT_AUTOUNAFK.getId()))
        {
            return;
        }
        afkCommand.updateLastAction(player);
    }

}
