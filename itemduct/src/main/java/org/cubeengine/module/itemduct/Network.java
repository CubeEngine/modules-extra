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
package org.cubeengine.module.itemduct;

import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.block.entity.carrier.Dropper;
import org.spongepowered.api.block.entity.carrier.chest.Chest;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.query.Query;
import org.spongepowered.api.item.inventory.query.QueryTypes;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.ServerLocation;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Network
{

    public Set<ServerLocation> pipes = new HashSet<>();
    public Map<ServerLocation, Map<Direction, List<ItemStack>>> exitPoints = new LinkedHashMap<>();
    public Set<ServerLocation> storage = new HashSet<>();
    public Set<ServerLocation> errors = new HashSet<>();
    private ItemDuctManager manager;

    public Network(ItemDuctManager manager)
    {
        this.manager = manager;
    }

    public void activate(Inventory inventory, List<ItemStack> filters)
    {
        if (isStorage())
        {
            pullFromStorage(inventory, filters);
            return;
        }
        for (Map.Entry<ServerLocation, Map<Direction, List<ItemStack>>> entry : exitPoints.entrySet())
        {
            Inventory pollFrom = inventory;
            ServerLocation loc = entry.getKey();
            Map<Direction, List<ItemStack>> data = entry.getValue();
            Direction dir = loc.get(Keys.DIRECTION).orElse(Direction.NONE).getOpposite();
            ServerLocation targetLoc = loc.add(dir.getOpposite().asBlockOffset());
            BlockEntity te = targetLoc.getBlockEntity().get();
            Inventory target = ((Carrier) te).getInventory();
            if (te instanceof Dropper)
            {
                Network nw = manager.findNetwork(targetLoc);
                nw.transferToStorage(queryFiltered(filters, inventory), data.get(dir));
                continue;
            }
            if (te instanceof Chest)
            {
                target = ((Chest) te).getDoubleChestInventory().orElse(target);
            }
            List<ItemStack> targetFilter = data.get(dir);
            if (targetFilter != null)
            {
                if (!filters.isEmpty()) // Only allow to extract items in the filter
                {
                    pollFrom = queryFiltered(filters, inventory); // TODO more filters
                }

                Inventory pollFromTo = pollFrom;
                if (!targetFilter.isEmpty()) // Only allow to insert items in the filter
                {
                    pollFromTo = queryFiltered(targetFilter, inventory);  // TODO more filters
                }
                // For all filtered slots
                doTransfer(pollFromTo, target);
            }
        }
    }

    private void transferToStorage(Inventory inventory, List<ItemStack> filters)
    {
        if (!filters.isEmpty()) // Only allow to extract items in the filter
        {
            inventory = queryFiltered(filters, inventory); // TODO more filters
        }
        for (ServerLocation targetLoc : storage)
        {
            BlockEntity te = targetLoc.getBlockEntity().get();
            Inventory target =  ((Carrier) te).getInventory();
            if (te instanceof Chest)
            {
                target = ((Chest) te).getDoubleChestInventory().orElse(target);
            }
            doTransfer(inventory, target);
        }
    }

    private Inventory queryFiltered(List<ItemStack> filters, Inventory inventory) {
        if (filters.isEmpty()) {
            return inventory;
        }
        final Query.Builder builder = Query.builder();
        for (ItemStack filter : filters) {
            builder.or(QueryTypes.ITEM_STACK_IGNORE_QUANTITY.get().of(filter));
        }
        return inventory.query(builder.build());
    }

    private void pullFromStorage(Inventory inventory, List<ItemStack> filters)
    {
        for (ServerLocation targetLoc : storage)
        {
            BlockEntity te = targetLoc.getBlockEntity().get();
            Inventory pollFrom =  ((Carrier) te).getInventory();
            if (te instanceof Chest)
            {
                pollFrom = ((Chest) te).getDoubleChestInventory().orElse(pollFrom);
            }
            if (!filters.isEmpty()) // Only allow to extract items in the filter
            {
                pollFrom = queryFiltered(filters, pollFrom); // TODO more filters
            }

            doTransfer(pollFrom, inventory);
        }
    }

    private void doTransfer(Inventory pollFrom, Inventory target)
    {
        for (Inventory slot : pollFrom.slots())
        {
            final ItemStack peek = slot.peek();
            ItemStack itemStack = peek.copy();
            // Try to insert into targetInventory
            target.offer(itemStack);
            // and poll the inserted amount
            if (itemStack.isEmpty())
            {
                slot.poll();
            }
            else
            {
                slot.poll(peek.getQuantity() - itemStack.getQuantity());
            }
        }
    }

    public boolean isStorage()
    {
        return !this.storage.isEmpty();
    }




}
