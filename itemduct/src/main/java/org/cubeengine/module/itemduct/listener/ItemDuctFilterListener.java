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

import static org.cubeengine.libcube.service.i18n.I18nTranslate.ChatType.ACTION_BAR;
import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;

import com.google.inject.Inject;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.inventoryguard.InventoryGuardFactory;
import org.cubeengine.libcube.service.permission.Permission;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.itemduct.Itemduct;
import org.cubeengine.module.itemduct.data.DuctData;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.HandType;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.item.inventory.ContainerTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.menu.InventoryMenu;
import org.spongepowered.api.item.inventory.type.ViewableInventory;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.ServerLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handles opening and saving ItemDuct Filters
 */
public class ItemDuctFilterListener
{
    @Inject private I18n i18n;
    @Inject private PermissionManager pm;

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
    public void onInteractPiston(InteractBlockEvent.Secondary event, @Root ServerPlayer player)
    {
        final Optional<HandType> usedHand = event.getContext().get(EventContextKeys.USED_HAND);
        if (usedHand.isPresent() && usedHand.get() == HandTypes.MAIN_HAND.get()) {
            event.getInteractionPoint().ifPresent(pos -> {
                final ServerLocation loc = player.getWorld().getLocation(pos);
                final Direction dir = loc.get(Keys.DIRECTION).orElse(Direction.NONE);
                final ServerLocation te = loc.add(dir.asBlockOffset());
                final Optional<Map<Direction, List<ItemStack>>> filters = te.get(DuctData.FILTERS);
                final ItemStack itemInHand = player.getItemInHand(HandTypes.MAIN_HAND);
                if (filters.isPresent() && itemInHand.isEmpty() && player.get(Keys.IS_SNEAKING).orElse(false)) {
                    openFilter(player, filters.get(), dir.getOpposite(), te);
                }
            });
        }
    }

    private void openFilter(ServerPlayer player, Map<Direction, List<ItemStack>> ductData, Direction dir, ServerLocation loc)
    {
        if (!player.hasPermission(viewFilterPerm.getId()))
        {
            i18n.send(ACTION_BAR, player, NEGATIVE, "You are not allowed to edit filters");
            return;
        }

        List<ItemStack> list = ductData.getOrDefault(dir, Collections.emptyList());

        final ViewableInventory inventory = ViewableInventory.builder().type(ContainerTypes.GENERIC_9x3).completeStructure().build();
        list.forEach(inventory::offer);
        final InventoryMenu menu = InventoryMenu.of(inventory);
        boolean canEdit = player.hasPermission(editFilterPerm.getId());
        menu.setReadOnly(!canEdit);
        menu.setTitle(canEdit ? i18n.translate(player, "ItemDuct Filters") :
                                i18n.translate(player, "View ItemDuct Filters"));
        menu.registerClose((cause, container) -> onClose(inventory, ductData, dir, loc));
        menu.open(player);
        player.getProgress(module.filters).grant();
    }

    private void onClose(ViewableInventory inventory, Map<Direction, List<ItemStack>> ductData, Direction dir, ServerLocation loc)
    {
        List<ItemStack> list1 = new ArrayList<>();
        for (Slot slot : inventory.slots()) {
            final ItemStack item = slot.peek();
            if (!item.isEmpty()) {
                list1.add(item);
            }
        }
        ductData.put(dir, list1);
        loc.offer(DuctData.FILTERS, ductData);
    }

}
