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
import static org.spongepowered.api.block.BlockTypes.PISTON;
import static org.spongepowered.api.block.BlockTypes.STICKY_PISTON;

import org.cubeengine.module.itemduct.data.ItemductBlocks;
import org.cubeengine.module.itemduct.data.ItemductData;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.block.entity.carrier.Dropper;
import org.spongepowered.api.block.entity.carrier.chest.Chest;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.type.DyeColor;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.query.Query;
import org.spongepowered.api.item.inventory.query.QueryTypes;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3i;

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

public class Network
{
    private static Set<Direction> directions = EnumSet.of(Direction.DOWN, Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

    private ItemductBlocks blocks;
    private int maxDepth;

    public Vector3i start;
    public Set<Vector3i> pipes = new HashSet<>();
    public Map<Vector3i, Map<Direction, List<ItemStack>>> exitPoints = new LinkedHashMap<>();
    public Set<Vector3i> storage = new HashSet<>();
    public Set<Vector3i> errors = new HashSet<>();
    public final ServerWorld world;

    public Network(ItemductBlocks blocks, ServerWorld world, int maxDepth)
    {
        this.blocks = blocks;
        this.world = world;
        this.maxDepth = maxDepth;
    }

    // Start with an exit/entry point
    public Network discover(Vector3i start) {
        this.start = start;
        Direction dir = world.get(start, Keys.DIRECTION).orElse(Direction.NONE);
        if (!world.block(start).type().isAnyOf(DROPPER))
        {
            dir = dir.opposite();
        }
        final Vector3i rel = start.add(dir.asBlockOffset());
        final BlockType relType = world.block(rel).type();
        if (blocks.isPipe(relType))
        {
            this.discover(rel, dir);
        }
        return this;
    }

    public void discover(Vector3i firstPipe, Direction dir) {
        pipes.add(firstPipe);
        discover(new Network.LastDuct(firstPipe, dir), 0);
    }

    private void discover(Network.LastDuct last, int depth)
    {
        if (depth > maxDepth)
        {
            this.errors.add(last.pos);
            return;
        }
        Map<Direction, Vector3i> map = new HashMap<>();
        Queue<Vector3i> next = new LinkedList<>();
        next.offer(last.pos);
        do
        {
            Set<Direction> dirs = directions;
            Set<Direction> connected = new HashSet<>();
            connected.add(last.from.opposite());
            for (Direction dir : dirs)
            {
                if (!dir.equals(last.from.opposite()))
                {
                    if (last.cross && last.color == null && !dir.equals(last.from)) {
                        continue;
                    }
                    Vector3i rel = last.pos.add(dir.asBlockOffset());
                    if (last.isCompatible(rel))
                    {
                        if (this.pipes.contains(rel)) // No loops allowed
                        {
                            this.errors.add(rel);
                            this.errors.add(last.pos);
                        }
                        else
                        {
                            this.pipes.add(rel);
                            map.put(dir, rel);
                        }
                        connected.add(dir);
                    }

                    // ExitPiston?
                    if (this.world.get(rel, Keys.DIRECTION).orElse(Direction.NONE).equals(dir))
                    {
                        if (this.world.block(rel).type().isAnyOf(PISTON))
                        {

                            final Vector3i relLoc = rel.add(dir.asBlockOffset());

                            if (this.world.get(relLoc, ItemductData.FILTERS).map(d -> d.get(dir.opposite()) != null).orElse(false))
                            {
                                this.exitPoints.put(rel, this.world.get(relLoc, ItemductData.FILTERS).get());
                                connected.add(dir);
                            }
                        }
                        else if (this.world.block(rel).type().isAnyOf(STICKY_PISTON))
                        {
                            connected.add(dir);
                        }

                    }
                    // Storage Chest
                    if (last.storage)
                    {
                        this.world.blockEntity(rel).ifPresent(te -> {
                            if (te instanceof Carrier) {
                                this.storage.add(rel);
                            }
                        });
                    }
                }

            }

            if (last.cross && last.color != null)
            {
                final BlockState state = world.block(last.pos).with(Keys.CONNECTED_DIRECTIONS, connected).get();
                world.setBlock(last.pos, state);
            }

            if (map.size() > 1)
            {
                for (Map.Entry<Direction, Vector3i> entry : map.entrySet())
                {
                    discover(new Network.LastDuct(entry.getValue(), entry.getKey()), depth +1);
                }
            }
            else if (map.size() == 1)
            {
                for (Map.Entry<Direction, Vector3i> entry : map.entrySet())
                {
                    last.update(entry.getValue(), entry.getKey());
                }
                next.offer(last.pos);
            }
            // else nothing found here
            next.poll();
            map.clear();
        }
        while (!next.isEmpty());

    }

    public void trigger(Inventory inventory, List<ItemStack> filters)
    {
        if (isStorage())
        {
            pullFromStorage(inventory, filters);
            return;
        }
        for (Map.Entry<Vector3i, Map<Direction, List<ItemStack>>> entry : exitPoints.entrySet())
        {
            Inventory pollFrom = inventory;
            Vector3i loc = entry.getKey();
            Map<Direction, List<ItemStack>> data = entry.getValue();
            Direction dir = world.get(loc, Keys.DIRECTION).orElse(Direction.NONE).opposite();
            final Vector3i targetLoc = loc.add(dir.opposite().asBlockOffset());
            BlockEntity te = world.blockEntity(targetLoc).get();
            Inventory target = ((Carrier) te).inventory();
            if (te instanceof Dropper)
            {
                Network nw = new Network(this.blocks, this.world, this.maxDepth).discover(targetLoc);
                nw.transferToStorage(queryFiltered(filters, inventory), data.get(dir));
                continue;
            }
            if (te instanceof Chest)
            {
                target = ((Chest) te).doubleChestInventory().orElse(target);
            }
            List<ItemStack> targetFilter = data.get(dir);
            if (targetFilter != null)
            {
                if (!filters.isEmpty()) // Only allow to extract items in the filter
                {
                    pollFrom = queryFiltered(filters, inventory);
                }

                Inventory pollFromTo = pollFrom;
                if (!targetFilter.isEmpty()) // Only allow to insert items in the filter
                {
                    pollFromTo = queryFiltered(targetFilter, inventory);
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
            inventory = queryFiltered(filters, inventory);
        }
        for (Vector3i targetLoc : storage)
        {
            BlockEntity te = world.blockEntity(targetLoc).get();
            Inventory target =  ((Carrier) te).inventory();
            if (te instanceof Chest)
            {
                target = ((Chest) te).doubleChestInventory().orElse(target);
            }
            doTransfer(inventory, target);
        }
    }

    // TODO Feature: more kind of item filters
    private Inventory queryFiltered(List<ItemStack> filters, Inventory inventory) {
        if (filters.isEmpty()) {
            return inventory;
        }
        final Query query = Query.orQueries(filters.stream().map(filter -> QueryTypes.ITEM_STACK_IGNORE_QUANTITY.get().of(filter)).toArray(Query[]::new));
        return inventory.query(query);
    }

    private void pullFromStorage(Inventory inventory, List<ItemStack> filters)
    {
        for (Vector3i targetLoc : storage)
        {
            BlockEntity te = world.blockEntity(targetLoc).get();
            Inventory pollFrom =  ((Carrier) te).inventory();
            if (te instanceof Chest)
            {
                pollFrom = ((Chest) te).doubleChestInventory().orElse(pollFrom);
            }
            if (!filters.isEmpty()) // Only allow to extract items in the filter
            {
                pollFrom = queryFiltered(filters, pollFrom);
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
                slot.poll(peek.quantity() - itemStack.quantity());
            }
        }
    }

    public boolean isStorage()
    {
        return !this.storage.isEmpty();
    }


    @Override
    public String toString() {
        return "Network in " + world.key() + " with " + exitPoints.size() + " exit-points and " + pipes.size() + " pipes and " + storage.size() + " storage";
    }

    class LastDuct
    {
        public Vector3i pos;
        public DyeColor color;
        public boolean cross;
        public Direction from;

        public boolean storage;

        public LastDuct(Vector3i loc, Direction from)
        {
            this.update(loc, from);
        }

        public void update(Vector3i loc, Direction from)
        {
            this.from = from;
            this.pos = loc;
            this.color = world.get(loc, Keys.DYE_COLOR).orElse(null);
            final BlockType blockType = world.block(loc).type();
            this.cross = blocks.isDirectionalPipe(blockType);
            this.storage = blocks.isStoragePipe(blockType);
        }

        public boolean isCompatible(Vector3i rel)
        {
            final BlockType relType = world.block(rel).type();
            final boolean relIsStorage = blocks.isStoragePipe(relType);
            if (!blocks.isNormalPipe(relType) && !relIsStorage)
            {
                return false;
            }
            if (storage)
            {
                return relIsStorage;
            }
            if (relIsStorage)
            {
                return false;
            }

            DyeColor color = world.get(rel, Keys.DYE_COLOR).orElse(null);
            return color == null || color == this.color || this.color == null;
        }

    }
}
