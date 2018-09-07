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

import org.cubeengine.module.itemduct.data.DuctData;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.carrier.Chest;
import org.spongepowered.api.block.tileentity.carrier.Dropper;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.query.QueryOperation;
import org.spongepowered.api.item.inventory.query.QueryOperationTypes;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Network
{

    public Set<Location<World>> pipes = new HashSet<>();
    public Map<Location<World>, DuctData> exitPoints = new LinkedHashMap<>();
    public Set<Location<World>> storage = new HashSet<>();
    public Set<Location<World>> errors = new HashSet<>();
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
        for (Map.Entry<Location<World>, DuctData> entry : exitPoints.entrySet())
        {
            Inventory pollFrom = inventory;
            Location<World> loc = entry.getKey();
            DuctData data = entry.getValue();
            Direction dir = loc.get(Keys.DIRECTION).orElse(Direction.NONE).getOpposite();
            Location<World> targetLoc = loc.getRelative(dir.getOpposite());
            TileEntity te = targetLoc.getTileEntity().get();
            Inventory target = ((Carrier) te).getInventory();
            if (te instanceof Dropper)
            {
                Network nw = manager.findNetwork(targetLoc);
                nw.transferToStorage(queryFiltered(filters, inventory), data.get(dir).get());
                continue;
            }
            if (te instanceof Chest)
            {
                target = ((Chest) te).getDoubleChestInventory().orElse(target);
            }
            Optional<List<ItemStack>> targetFilter = data.get(dir);
            if (targetFilter.isPresent())
            {
                if (!filters.isEmpty()) // Only allow to extract items in the filter
                {
                    pollFrom = queryFiltered(filters, inventory); // TODO more filters
                }

                Inventory pollFromTo = pollFrom;
                if (!targetFilter.get().isEmpty()) // Only allow to insert items in the filter
                {
                    pollFromTo = queryFiltered(targetFilter.get(), inventory);  // TODO more filters
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
        for (Location<World> targetLoc : storage)
        {
            TileEntity te = targetLoc.getTileEntity().get();
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
        QueryOperation[] ops = new QueryOperation[filters.size()];
        for (int i = 0; i < filters.size(); i++) {
            ItemStack filter = filters.get(i);
            ops[i] = QueryOperationTypes.ITEM_STACK_IGNORE_QUANTITY.of(filter);
        }
        return inventory.query(ops);
    }

    private void pullFromStorage(Inventory inventory, List<ItemStack> filters)
    {
        for (Location<World> targetLoc : storage)
        {
            TileEntity te = targetLoc.getTileEntity().get();
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
            Optional<ItemStack> peek = slot.peek();
            if (peek.isPresent())
            {
                ItemStack itemStack = peek.get().copy();
                // Try to insert into targetInventory
                target.offer(itemStack);
                // and poll the inserted amount
                if (itemStack.isEmpty())
                {
                    slot.poll();
                }
                else
                {
                    slot.poll(peek.get().getQuantity() - itemStack.getQuantity());
                }
            }
        }
    }

    public boolean isStorage()
    {
        return !this.storage.isEmpty();
    }




}
