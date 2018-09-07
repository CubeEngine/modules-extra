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

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;

import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.inventoryguard.InventoryGuardFactory;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.itemduct.DuctFilterCarrier;
import org.cubeengine.module.itemduct.Itemduct;
import org.cubeengine.module.itemduct.data.DuctData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.InteractInventoryEvent;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.property.InventoryTitle;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.format.TextFormat;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

/**
 * Handles opening and saving ItemDuct Filters
 */
public class ItemDuctFilterListener
{
    @Inject private PluginContainer plugin;
    @Inject private I18n i18n;
    @Inject private PermissionManager pm;
    @Inject private InventoryGuardFactory igf;

    private Permission viewFilterPerm;
    private Permission editFilterPerm;
    private Itemduct module;

    public void setup(Itemduct module)
    {
        this.module = module;
        viewFilterPerm = this.pm.register(Itemduct.class, "filter.view", "Allows viewing ItemDuct Filters", null);
        editFilterPerm = this.pm.register(Itemduct.class, "filter.edit", "Allows editing ItemDuct Filters", null);
    }

    @Listener
    public void onInteractPiston(InteractBlockEvent.Secondary.MainHand event, @Root Player player)
    {
        event.getTargetBlock().getLocation().ifPresent(loc -> {
            Direction dir = loc.get(Keys.DIRECTION).orElse(Direction.NONE);
            Location<World> te = loc.getRelative(dir);
            Optional<DuctData> ductData = te.get(DuctData.class);
            Optional<ItemStack> itemInHand = player.getItemInHand(HandTypes.MAIN_HAND);
            if (ductData.isPresent() && itemInHand.map(ItemStack::isEmpty).orElse(false) && player.get(Keys.IS_SNEAKING).orElse(false))
            {
                openFilter(player, ductData.get(), dir.getOpposite(), te);
            }
        });
    }

    @Listener
    public void onCloseInventory(InteractInventoryEvent.Close event, @Root Player player, @Getter("getTargetInventory") Container inventory)
    {
        // When closing update filters
        if (inventory instanceof CarriedInventory<?>) {
            ((CarriedInventory<?>) inventory).getCarrier().ifPresent(carrier -> {
                if (carrier instanceof DuctFilterCarrier)
                {
                    ((DuctFilterCarrier) carrier).update(event.getTargetInventory().iterator().next());
                }
            });
        }
    }

    private void openFilter(Player player, DuctData ductData, Direction dir, Location<World> loc)
    {
        if (!player.hasPermission(viewFilterPerm.getId()))
        {
            i18n.send(ChatTypes.ACTION_BAR, player, NEGATIVE, "You are not allowed to edit filters");
            return;
        }

        List<ItemStack> list = ductData.get(dir).get();
        DuctFilterCarrier carrier = new DuctFilterCarrier(ductData, loc, dir);
        boolean canEdit = player.hasPermission(editFilterPerm.getId());
        Inventory inventory = Inventory.builder()
                .property(InventoryTitle.PROPERTY_NAME,
                        InventoryTitle.of(canEdit ? i18n.translate(player, TextFormat.NONE, "ItemDuct Filters") :
                                                    i18n.translate(player, TextFormat.NONE, "View ItemDuct Filters")))
                .withCarrier(carrier)
                .build(plugin);
        carrier.init(((CarriedInventory<?>) inventory));

        for (ItemStack itemStack : list)
        {
            inventory.offer(itemStack);
        }

        Sponge.getCauseStackManager().pushCause(player);
        if (canEdit)
        {
            player.openInventory(inventory);
        }
        else
        {
            igf.prepareInv(inventory, player.getUniqueId()).blockPutInAll().blockTakeOutAll().submitInventory(Itemduct.class, true);
        }
        player.getProgress(module.filters).grant();
    }

}
