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
package org.cubeengine.module.itemrepair.material;

import java.util.HashMap;
import java.util.Map;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;

import static org.spongepowered.api.item.ItemTypes.*;

public class RepairItemContainer
{
    private final Map<ItemType, RepairItem> repairItems = new HashMap<>();
    private final BaseMaterialContainer baseMat;

    public RepairItemContainer(BaseMaterialContainer baseMat)
    {
        this.baseMat = baseMat;
        this.registerDefaultRepairItems();
    }

    private void registerDefaultRepairItems()
    {
        // TOOLS
        // IRON
        this.registerRepairItem(RepairItem.of(IRON_SHOVEL, baseMat.of(IRON_INGOT), 1))
            .registerRepairItem(RepairItem.of(IRON_PICKAXE,baseMat.of(IRON_INGOT),3))
            .registerRepairItem(RepairItem.of(IRON_AXE,baseMat.of(IRON_INGOT),3))
            .registerRepairItem(RepairItem.of(FLINT_AND_STEEL,baseMat.of(IRON_INGOT),1))
            .registerRepairItem(RepairItem.of(IRON_SWORD,baseMat.of(IRON_INGOT),2))
            .registerRepairItem(RepairItem.of(IRON_HOE,baseMat.of(IRON_INGOT),2))
            .registerRepairItem(RepairItem.of(SHEARS,baseMat.of(IRON_INGOT),2))
        // WOOD
            .registerRepairItem(RepairItem.of(BOW, baseMat.of(LOG), 2))
            .registerRepairItem(RepairItem.of(WOODEN_SWORD, baseMat.of(LOG), 2))
            .registerRepairItem(RepairItem.of(WOODEN_SHOVEL, baseMat.of(LOG), 1))
            .registerRepairItem(RepairItem.of(WOODEN_PICKAXE, baseMat.of(LOG), 3))
            .registerRepairItem(RepairItem.of(WOODEN_AXE, baseMat.of(LOG), 3))
            .registerRepairItem(RepairItem.of(WOODEN_HOE, baseMat.of(LOG), 2))
            .registerRepairItem(RepairItem.of(FISHING_ROD, baseMat.of(LOG), 2))
        // STONE
            .registerRepairItem(RepairItem.of(STONE_SWORD, baseMat.of(STONE), 2))
            .registerRepairItem(RepairItem.of(STONE_SHOVEL, baseMat.of(STONE), 1))
            .registerRepairItem(RepairItem.of(STONE_PICKAXE, baseMat.of(STONE), 3))
            .registerRepairItem(RepairItem.of(STONE_AXE, baseMat.of(STONE), 3))
        // DIAMOND
            .registerRepairItem(RepairItem.of(DIAMOND_SWORD, baseMat.of(DIAMOND), 2))
            .registerRepairItem(RepairItem.of(DIAMOND_SHOVEL, baseMat.of(DIAMOND), 1))
            .registerRepairItem(RepairItem.of(DIAMOND_PICKAXE, baseMat.of(DIAMOND), 3))
            .registerRepairItem(RepairItem.of(DIAMOND_AXE, baseMat.of(DIAMOND), 3))
            .registerRepairItem(RepairItem.of(DIAMOND_HOE, baseMat.of(DIAMOND), 2))
        // GOLD
            .registerRepairItem(RepairItem.of(GOLDEN_SWORD, baseMat.of(GOLD_INGOT), 2))
            .registerRepairItem(RepairItem.of(GOLDEN_SHOVEL, baseMat.of(GOLD_INGOT), 1))
            .registerRepairItem(RepairItem.of(GOLDEN_PICKAXE, baseMat.of(GOLD_INGOT), 3))
            .registerRepairItem(RepairItem.of(GOLDEN_AXE, baseMat.of(GOLD_INGOT), 3))
            .registerRepairItem(RepairItem.of(GOLDEN_HOE, baseMat.of(GOLD_INGOT), 2))
        // ARMOR
        // LEATHER
            .registerRepairItem(RepairItem.of(LEATHER_HELMET, baseMat.of(LEATHER), 5))
            .registerRepairItem(RepairItem.of(LEATHER_CHESTPLATE, baseMat.of(LEATHER), 8))
            .registerRepairItem(RepairItem.of(LEATHER_LEGGINGS, baseMat.of(LEATHER), 7))
            .registerRepairItem(RepairItem.of(LEATHER_BOOTS, baseMat.of(LEATHER), 4))
        // CHAINMAIL
            .registerRepairItem(RepairItem.of(CHAINMAIL_HELMET, baseMat.of(IRON_BARS), 5))
            .registerRepairItem(RepairItem.of(CHAINMAIL_CHESTPLATE, baseMat.of(IRON_BARS), 8))
            .registerRepairItem(RepairItem.of(CHAINMAIL_LEGGINGS, baseMat.of(IRON_BARS), 7))
            .registerRepairItem(RepairItem.of(CHAINMAIL_BOOTS, baseMat.of(IRON_BARS), 4))
        // IRON
            .registerRepairItem(RepairItem.of(IRON_HELMET, baseMat.of(IRON_INGOT), 5))
            .registerRepairItem(RepairItem.of(IRON_CHESTPLATE, baseMat.of(IRON_INGOT), 8))
            .registerRepairItem(RepairItem.of(IRON_LEGGINGS, baseMat.of(IRON_INGOT), 7))
            .registerRepairItem(RepairItem.of(IRON_BOOTS, baseMat.of(IRON_INGOT), 4))
        // DIAMOND
            .registerRepairItem(RepairItem.of(DIAMOND_HELMET, baseMat.of(DIAMOND), 5))
            .registerRepairItem(RepairItem.of(DIAMOND_CHESTPLATE, baseMat.of(DIAMOND), 8))
            .registerRepairItem(RepairItem.of(DIAMOND_LEGGINGS, baseMat.of(DIAMOND), 7))
            .registerRepairItem(RepairItem.of(DIAMOND_BOOTS, baseMat.of(DIAMOND), 4))
        // GOLD
            .registerRepairItem(RepairItem.of(GOLDEN_HELMET, baseMat.of(GOLD_INGOT), 5))
            .registerRepairItem(RepairItem.of(GOLDEN_CHESTPLATE, baseMat.of(GOLD_INGOT), 8))
            .registerRepairItem(RepairItem.of(GOLDEN_LEGGINGS, baseMat.of(GOLD_INGOT), 7))
            .registerRepairItem(RepairItem.of(GOLDEN_BOOTS, baseMat.of(GOLD_INGOT), 4));
    }

    public Map<Integer, ItemStack> getRepairableItems(Inventory inventory)
    {
        Map<Integer, ItemStack> items = new HashMap<>();

        ItemStack item;
        for (int i = 0; i < inventory.getSize(); ++i)
        {
            item = inventory.getItem(i);
            if (item != null && this.of(item.getType()) != null && item.getDurability() > 0)
            {
                items.put(i, item);
            }
        }

        return items;
    }

    public RepairItemContainer registerRepairItem(RepairItem repairItem)
    {
        if (repairItem == null) return this;
        this.repairItems.put(repairItem.getMaterial(),repairItem);
        return this;
    }

    public BaseMaterialContainer getPriceProvider()
    {
        return this.baseMat;
    }

    public RepairItem of(ItemType mat)
    {
        return this.repairItems.get(mat);
    }
}
