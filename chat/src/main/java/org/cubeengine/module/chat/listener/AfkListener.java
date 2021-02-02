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
package org.cubeengine.module.chat.listener;

import org.cubeengine.module.chat.ChatPerm;
import org.cubeengine.module.chat.command.AfkCommand;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.command.ExecuteCommandEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.container.InteractContainerEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;

import static org.spongepowered.api.event.Order.POST;

public class AfkListener
{
    private final AfkCommand afkCommand;
    private ChatPerm perms;

    public AfkListener(AfkCommand afkCommand, ChatPerm perms)
    {
        this.afkCommand = afkCommand;
        this.perms = perms;
    }

    @Listener(order = POST)
    public void onMove(MoveEntityEvent event, @Getter("getEntity") ServerPlayer player)
    {
        if (event.getOriginalPosition().getFloorX() == event.getDestinationPosition().getFloorX()
            && event.getOriginalPosition().getFloorZ() == event.getDestinationPosition().getFloorZ())
        {
            return;
        }
        this.updateLastAction(player);
    }

    @Listener(order = POST)
    public void onInventoryInteract(InteractContainerEvent event, @Root ServerPlayer player)
    {
        this.updateLastAction(player);
    }

    @Listener(order = POST)
    public void playerInteract(InteractBlockEvent event, @Root ServerPlayer player)
    {
        this.updateLastAction(player);
    }


    @Listener(order = POST)
    public void playerInteract(InteractEntityEvent event, @Root ServerPlayer player)
    {
        this.updateLastAction(player);
    }

    @Listener(order = POST)
    public void onChat(MessageChannelEvent event, @Root ServerPlayer player)
    {
        this.updateLastAction(player);
        afkCommand.checkAfk();
    }

    @Listener(order = POST)
    public void onCommand(ExecuteCommandEvent.Pre event, @Root ServerPlayer player)
    {
        this.updateLastAction(player);
    }

    // TODO new Event?
//    @Listener(order = POST)
//    public void onTabComplete(TabCompleteEvent event, @Root ServerPlayer player)
//    {
//        this.updateLastAction(player);
//    }

    @Listener(order = POST)
    public void onLeave(ServerSideConnectionEvent.Disconnect event, @Root ServerPlayer player)
    {
        afkCommand.setAfk(player, false);
        afkCommand.resetLastAction(player);
    }

    // TODO new Event?
//    @Listener(order = POST)
//    public void onBowShot(LaunchProjectileEvent event, @Root ServerPlayer source)
//    {
//        this.updateLastAction(source);
//    }

    private void updateLastAction(ServerPlayer player)
    {
        if (afkCommand.isAfk(player) && perms.PREVENT_AUTOUNAFK.check(player))
        {
            return;
        }
        afkCommand.updateLastAction(player);
    }

}
