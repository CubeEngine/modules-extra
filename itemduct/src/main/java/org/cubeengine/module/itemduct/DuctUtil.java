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

import static org.spongepowered.api.block.BlockTypes.DROPPER;
import static org.spongepowered.api.block.BlockTypes.GLASS_PANE;
import static org.spongepowered.api.block.BlockTypes.PISTON;
import static org.spongepowered.api.block.BlockTypes.QUARTZ_BLOCK;
import static org.spongepowered.api.block.BlockTypes.STAINED_GLASS_PANE;

import org.cubeengine.module.itemduct.data.DuctData;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.carrier.Chest;
import org.spongepowered.api.block.tileentity.carrier.Dropper;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.DyeColor;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

public class DuctUtil
{
    private static Set<BlockType> pipeTypes = new HashSet<>();
    static
    {
        pipeTypes.add(BlockTypes.GLASS);
        pipeTypes.add(GLASS_PANE);
        pipeTypes.add(BlockTypes.STAINED_GLASS);
        pipeTypes.add(STAINED_GLASS_PANE);
        pipeTypes.add(QUARTZ_BLOCK);
    }
    private static Set<Direction> directions = EnumSet.of(Direction.DOWN, Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

    private static int maxDepth = 10;

    public static Network findNetwork(Location<World> start)
    {
        Direction dir = start.get(Keys.DIRECTION).orElse(Direction.NONE);
        if ((!start.getBlockType().equals(DROPPER)))
        {
            dir = dir.getOpposite();
        }
        Network network = new Network();
        if (pipeTypes.contains(start.getRelative(dir).getBlockType()))
        {
            start = start.getRelative(dir);
            network.pipes.add(start);
            findNetwork(new LastDuct(start, dir), network, 0);
        }
        return network;
    }

    private static void findNetwork(LastDuct last, Network network, int depth)
    {
        if (depth > maxDepth)
        {
            network.errors.add(last.loc);
            return;
        }
        Map<Direction, Location<World>> map = new HashMap<>();
        Queue<Location<World>> next = new LinkedList<>();
        next.offer(last.loc);
        do
        {
            // TODO limit direction with glass panes
            Set<Direction> dirs = directions;
            for (Direction dir : dirs)
            {
                if (!dir.equals(last.from.getOpposite()))
                {
                    Location<World> rel = last.loc.getRelative(dir);
                    if (last.isCompatible(rel))
                    {
                        if (network.pipes.contains(rel)) // No loops allowed
                        {
                            network.errors.add(rel);
                            network.errors.add(last.loc);
                        }
                        else
                        {
                            network.pipes.add(rel);
                            map.put(dir, rel);
                        }
                    }
                    // ExitPiston?
                    if (rel.getBlockType().equals(PISTON) && rel.get(Keys.DIRECTION).orElse(Direction.NONE).equals(dir))
                    {
                        if (rel.getRelative(dir).get(DuctData.class).map(d -> d.get(dir.getOpposite()).isPresent()).orElse(false))
                        {
                            network.exitPoints.put(rel, rel.getRelative(dir).get(DuctData.class).get());
                        }
                    }
                    // Storage Chest
                    if (last.storage)
                    {
                        rel.getTileEntity().ifPresent(te -> {
                            if (te instanceof Carrier) {
                                network.storage.add(rel);
                            }
                        });
                    }
                }

            }

            if (map.size() > 1)
            {
                for (Map.Entry<Direction, Location<World>> entry : map.entrySet())
                {
                    findNetwork(new LastDuct(entry.getValue(), entry.getKey()), network, depth + 1);
                }
            }
            else if (map.size() == 1)
            {
                for (Map.Entry<Direction, Location<World>> entry : map.entrySet())
                {
                    last.update(entry.getValue(), entry.getKey());
                }
                next.offer(last.loc);
            }
            // else nothing found here
            next.poll();
            map.clear();
        }
        while (!next.isEmpty());

    }

    private static class LastDuct
    {
        public Location<World> loc;
        public DyeColor color;
        public boolean cross;
        public Direction from;
        public boolean storage;

        public LastDuct(Location<World> loc, Direction from)
        {
            this.update(loc, from);
        }

        public void update(Location<World> loc, Direction from)
        {
            this.from = from;
            this.loc = loc;
            this.color = loc.get(Keys.DYE_COLOR).orElse(null);
            this.cross = GLASS_PANE.equals(loc.getBlockType()) || STAINED_GLASS_PANE.equals(loc.getBlockType());
            this.storage = loc.getBlockType().equals(QUARTZ_BLOCK);
        }

        public boolean isCompatible(Location<World> rel)
        {
            if (!pipeTypes.contains(rel.getBlockType()))
            {
                return false;
            }
            if (storage)
            {
                return rel.getBlockType().equals(QUARTZ_BLOCK);
            }
            DyeColor color = rel.get(Keys.DYE_COLOR).orElse(null);
            return color == null || color == this.color || this.color == null;
        }
    }

    public static class Network
    {
        public Set<Location<World>> pipes = new HashSet<>();
        public Map<Location<World>, DuctData> exitPoints = new LinkedHashMap<>();
        public Set<Location<World>> storage = new HashSet<>();
        public Set<Location<World>> errors = new HashSet<>();

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
                    Network nw = findNetwork(targetLoc);
                    nw.transferToStorage(inventory, filters);
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
                        pollFrom = inventory.queryAny(filters.toArray(new ItemStack[filters.size()])); // TODO more filters
                    }

                    Inventory pollFromTo = pollFrom;
                    if (!targetFilter.get().isEmpty()) // Only allow to insert items in the filter
                    {
                        pollFromTo = pollFromTo.queryAny(targetFilter.get().toArray(new ItemStack[targetFilter.get().size()]));  // TODO more filters
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
                inventory = inventory.queryAny(filters.toArray(new ItemStack[filters.size()])); // TODO more filters
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
                    pollFrom = pollFrom.queryAny(filters.toArray(new ItemStack[filters.size()])); // TODO more filters
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
}
