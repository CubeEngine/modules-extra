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

import org.cubeengine.module.itemduct.data.ItemductBlocks;
import org.cubeengine.module.itemduct.data.ItemductData;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.ServerLocation;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3i;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkFilter {

    public final ServerLocation filterLoc;
    public final ServerLocation inventoryLoc;
    public final Direction filterDir;
    private Map<Direction, List<ItemStack>> filters;
    public final boolean validType;

    public NetworkFilter(ServerWorld world, Vector3i pos) {
        this.filterLoc = world.getLocation(pos);
        final Direction dir = this.filterLoc.get(Keys.DIRECTION).orElse(Direction.NONE);
        this.filterDir = dir.getOpposite();
        this.inventoryLoc = this.filterLoc.relativeTo(dir);
        this.filters = this.inventoryLoc.get(ItemductData.FILTERS).orElse(null);
        this.validType = dir != Direction.NONE
                && ItemductBlocks.isEndPointType(this.filterLoc.getBlockType())
                && this.inventoryLoc.getBlockEntity().map(be -> be instanceof Carrier).orElse(false);
    }

    public boolean isValid() {
        return this.validType;
    }

    public boolean isActive() {
        return this.filters != null && this.filters.containsKey(this.filterDir);
    }

    public List<ItemStack> getFilterStacks() {
        return this.filters == null ? Collections.emptyList() : this.filters.get(this.filterDir);
    }

    public List<ItemStack> removeFilterStacks() {
        if (this.filters != null) {
            final List<ItemStack> removed = this.filters.remove(this.filterDir);
            if (this.filters.isEmpty()) {
                this.inventoryLoc.remove(ItemductData.FILTERS);
            } else {
                this.inventoryLoc.offer(ItemductData.FILTERS, this.filters);
            }
            return removed == null ? Collections.emptyList() : removed;
        }
        return Collections.emptyList();
    }

    public void setFilterStacks(List<ItemStack> list) {
        if (this.filters == null) {
            this.filters = new HashMap<>();
        }
        this.filters.put(this.filterDir, list);
        this.inventoryLoc.offer(ItemductData.FILTERS, this.filters);
    }
}
