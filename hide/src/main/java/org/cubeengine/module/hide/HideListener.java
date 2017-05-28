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
package org.cubeengine.module.hide;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.event.server.ClientPingServerEvent;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.profile.GameProfileManager;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

public class HideListener
{
    private final Hide module;
    private I18n i18n;

    public HideListener(Hide module, I18n i18n)
    {
        this.module = module;
        this.i18n = i18n;
    }

    @Listener
    public void onJoin(ClientConnectionEvent.Join event)
    {
        Player player = event.getTargetEntity();
        if (module.isHidden(player))
        {
            module.hidePlayer(player);
        }

        if (player.hasPermission(module.perms().AUTO_HIDE.getId()))
        {
            event.setMessageCancelled(true);
            module.hidePlayer(player);
            i18n.send(player, POSITIVE, "You were automatically hidden!");
        }
    }
    
    @Listener
    public void onQuit(ClientConnectionEvent.Disconnect event)
    {
        
        if (module.isHidden(event.getTargetEntity()))
        {
            event.setMessageCancelled(true);
            module.showPlayer(event.getTargetEntity());
        }
    }

    @Listener
    public void onInteract(InteractEvent event, @First Player player)
    {
        if (module.isHidden(player) && !player.hasPermission(module.perms().INTERACT.getId()))
        {
            event.setCancelled(true);
        }
    }

    @Listener
    public void onPickupItem(ChangeInventoryEvent.Pickup event, @First Player player)
    {
        if (module.isHidden(player) && !player.hasPermission(module.perms().PICKUP.getId()))
        {
            event.setCancelled(true);
        }
    }

    @Listener
    public void onChat(MessageChannelEvent.Chat event, @First Player player)
    {
        if (module.isHidden(player) && !player.hasPermission(module.perms().CHAT.getId()))
        {
            event.setCancelled(true);
        }
    }


    @Listener
    public void onDropItem(DropItemEvent event, @First Player player)
    {
        if (module.isHidden(player) && !player.hasPermission(module.perms().DROP.getId()))
        {
            event.setCancelled(true);
        }
    }

    @Listener
    public void onServerPingList(ClientPingServerEvent event)
    {
        GameProfileManager gpm = Sponge.getServer().getGameProfileManager();
        event.getResponse().getPlayers().ifPresent(l -> {
            for (UUID uuid : module.getHiddenUsers())
            {
                try
                {
                    GameProfile gp = gpm.get(uuid).get();
                    l.getProfiles().remove(gp);
                }
                catch (InterruptedException | ExecutionException e)
                {}
            }
        });
    }
}
