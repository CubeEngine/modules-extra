/**
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
package org.cubeengine.module.backpack;

import static org.spongepowered.api.item.inventory.InventoryArchetypes.CHEST;

import org.spongepowered.api.item.inventory.Carrier;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.property.InventoryDimension;
import org.spongepowered.api.item.inventory.property.InventoryTitle;
import org.spongepowered.api.item.inventory.type.CarriedInventory;
import org.spongepowered.api.text.Text;

public class BackpackHolder implements Carrier
{
    public final Inventory inventory;
    private final BackpackInventory backBackInventories;

    public BackpackHolder(BackpackInventory backBackInventories, String title)
    {
        this.backBackInventories = backBackInventories;
        this.inventory =
                Inventory.builder().of(CHEST)
                .property(InventoryDimension.PROPERTY_NAME, InventoryDimension.of(9, 6))
                .property(InventoryTitle.PROPERTY_NAME, InventoryTitle.of(Text.of(title)))
                .build(backBackInventories.module.getPlugin());
    }

    @Override
    public CarriedInventory<BackpackHolder> getInventory()
    {
        return ((CarriedInventory<BackpackHolder>) this.inventory);
    }

    public BackpackInventory getBackpack()
    {
        return this.backBackInventories;
    }
}
