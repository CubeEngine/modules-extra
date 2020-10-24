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
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.message.PlayerChatEvent;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.event.server.ClientPingServerEvent;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.profile.GameProfileManager;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

@Singleton
public class HideListener
{
    private final Hide module;
    private I18n i18n;
    private HidePerm perms;

    @Inject
    public HideListener(Hide module, I18n i18n, HidePerm perms)
    {
        this.module = module;
        this.i18n = i18n;
        this.perms = perms;
    }

    @Listener
    public void onJoin(ServerSideConnectionEvent.Join event)
    {
        ServerPlayer player = event.getPlayer();
        if (module.isHidden(player))
        {
            module.hidePlayer(player, true);
        }

        if (player.hasPermission(perms.AUTO_HIDE.getId()))
        {
            event.setMessageCancelled(true);
            module.hidePlayer(player, true);
            i18n.send(player, POSITIVE, "You were automatically hidden!");
        }
    }
    
    @Listener
    public void onQuit(ServerSideConnectionEvent.Disconnect event)
    {
        if (module.isHidden(event.getPlayer()))
        {
            event.setMessage(Component.empty());
            // TODO? event.setMessageCancelled(true);
            module.showPlayer(event.getPlayer(), true);
        }
    }

    @Listener
    public void onInteract(InteractEvent event, @First ServerPlayer player)
    {
        if (module.isHidden(player) && !player.hasPermission(perms.INTERACT.getId()))
        {
            event.setCancelled(true);
        }
    }

    @Listener
    public void onPickupItem(ChangeInventoryEvent.Pickup event, @First ServerPlayer player)
    {
        if (module.isHidden(player) && !player.hasPermission(perms.PICKUP.getId()))
        {
            event.setCancelled(true);
        }
    }

    @Listener
    public void onChat(PlayerChatEvent event, @First ServerPlayer player)
    {
        if (module.isHidden(player) && !player.hasPermission(perms.CHAT.getId()))
        {
            event.setCancelled(true);
        }
    }


    @Listener
    public void onDropItem(DropItemEvent event, @First ServerPlayer player)
    {
        if (module.isHidden(player) && !player.hasPermission(perms.DROP.getId()))
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
                GameProfile gp = gpm.get(uuid).join();
                l.getProfiles().remove(gp);
            }
        });
    }
}
