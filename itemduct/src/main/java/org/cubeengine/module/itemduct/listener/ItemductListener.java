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
package org.cubeengine.module.itemduct.listener;

import com.google.inject.Inject;
import org.cubeengine.module.itemduct.ItemductManager;
import org.cubeengine.module.itemduct.NetworkFilter;
import org.cubeengine.module.itemduct.data.ItemductBlocks;
import org.cubeengine.module.itemduct.data.ItemductData;
import org.cubeengine.module.itemduct.data.ItemductItems;
import org.cubeengine.libcube.util.ItemUtil;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.transaction.Operations;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.TransferInventoryEvent;
import org.spongepowered.api.event.item.inventory.container.InteractContainerEvent;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.math.vector.Vector3i;

import java.util.List;

/**
 * Handles ItemDuct setup and cleanup and effects
 */
public class ItemductListener
{
    @Inject private ItemductManager manager;

    @Listener
    public void onInteractPiston(InteractBlockEvent.Secondary event, @Root ServerPlayer player)
    {
        if (event.getContext().get(EventContextKeys.USED_HAND).map(hand -> hand != HandTypes.MAIN_HAND.get()).orElse(true)) {
            return;
        }
        final Vector3i pos = event.getBlock().getPosition();
        final ItemStack itemInHand = player.getItemInHand(HandTypes.MAIN_HAND);
        if (itemInHand.isEmpty() || ItemductItems.isActivator(itemInHand)) {
            final NetworkFilter networkFilter = new NetworkFilter(player.getWorld(), pos.toInt());
            if (!itemInHand.isEmpty()) {
                event.setCancelled(true); // Activator in hand - we do not want to ever place it as a normal block
                if (networkFilter.isValid() && !networkFilter.isActive()) {
                    manager.activateNetwork(event, player, itemInHand, networkFilter);
                }
            } else if (networkFilter.isActive()) {
                if (player.get(Keys.IS_SNEAKING).orElse(false)) {
                    manager.openFilter(player, networkFilter);
                } else {
                    manager.playNetworkEffects(networkFilter.filterLoc);
                }
            }
        }
    }

    @Listener
    public void onBreak(ChangeBlockEvent.All event)
    {
        event.getTransactions(Operations.BREAK.get()).filter(trans -> trans.getOriginal().getLocation().isPresent()).forEach(trans -> {
            BlockType type = trans.getOriginal().getState().getType();
            ServerLocation loc = trans.getOriginal().getLocation().get();
            if (ItemductBlocks.isEndPointType(type)) {
                final NetworkFilter networkFilter = new NetworkFilter(loc.getWorld(), loc.getBlockPosition());
                if (networkFilter.isValid()) {
                    final List<ItemStack> stacks = networkFilter.removeFilterStacks();
                    event.getCause().first(Player.class)
                            .filter(p -> p.get(Keys.GAME_MODE).map(mode -> mode != GameModes.CREATIVE).orElse(false))
                            .ifPresent(p -> {
                                ItemUtil.spawnItem(loc, ItemductItems.singleActivatorItem);
                                stacks.forEach(stack -> ItemUtil.spawnItem(loc, stack));
                            });
                }
            } else {
                loc.get(ItemductData.FILTERS).ifPresent(filters -> {
                    filters.values().forEach(stacks -> stacks.forEach(stack -> ItemUtil.spawnItem(loc, stack)));
                });
                loc.remove(ItemductData.FILTERS);
            }
        });
    }

    @Listener
    public void onCloseInventory(InteractContainerEvent.Close event, @Root ServerPlayer player, @Getter("getContainer") Container inventory)
    {
        // When closing update filters and prompt activation
        if (inventory instanceof CarriedInventory && inventory.totalQuantity() > 0)
        {
            ((CarriedInventory<?>) inventory).getCarrier().ifPresent(carrier -> this.manager.promptActivation(carrier, true, player));
        }
    }

    @Listener
    public void onOpenInventory(InteractContainerEvent.Open event, @Root ServerPlayer player, @Getter("getContainer") Container inventory)
    {
        // When opening prompt activation
        if (inventory instanceof CarriedInventory && inventory.totalQuantity() > 0)
        {
            ((CarriedInventory<?>) inventory).getCarrier().ifPresent(carrier -> this.manager.promptActivation(carrier, false, player));
        }
    }

    @Listener
    public void onTransferInventory(TransferInventoryEvent.Pre event)
    {
        // When getting items transferred prompt activation
        if (event.getTargetInventory() instanceof CarriedInventory)
        {
            ((CarriedInventory<?>) event.getTargetInventory()).getCarrier().ifPresent(c -> this.manager.promptActivation(c, true, null));
        }
    }

}
