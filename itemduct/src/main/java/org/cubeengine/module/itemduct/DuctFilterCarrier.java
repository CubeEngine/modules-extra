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
import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.List;

public class DuctFilterCarrier implements Carrier
{

    private CarriedInventory<? extends Carrier> inventory;

    private final DuctData ductData;
    private final Location<World> loc;
    private final Direction dir;

    public DuctFilterCarrier(DuctData ductData, Location<World> loc, Direction dir)
    {
        this.ductData = ductData;
        this.loc = loc;
        this.dir = dir;
    }

    public void init(CarriedInventory<? extends Carrier> inventory)
    {
        this.inventory = inventory;
    }

    @Override
    public CarriedInventory<? extends Carrier> getInventory()
    {
        return this.inventory;
    }

    public void update(Inventory inventory)
    {
        List<ItemStack> list = this.ductData.get(this.dir).get();
        list.clear();
        for (Inventory item : inventory.slots())
        {
            if (item.peek().isPresent())
            {
                list.add(item.peek().get());
            }
        }
        this.loc.offer(this.ductData);
    }
}
