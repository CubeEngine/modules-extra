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
package org.cubeengine.module.itemrepair.material;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;

public class BaseMaterialContainer
{
    private final Map<ItemType, BaseMaterial> baseMaterials = new HashMap<>();

    public BaseMaterialContainer()
    {
        this.registerBaseMaterial(new BaseMaterial(ItemTypes.LOG, 0.30));
        this.registerBaseMaterial(new BaseMaterial(ItemTypes.STONE, 1.00));
        this.registerBaseMaterial(new BaseMaterial(ItemTypes.IRON_INGOT, 2.10));
        this.registerBaseMaterial(new BaseMaterial(ItemTypes.GOLD_INGOT, 4.10));
        this.registerBaseMaterial(new BaseMaterial(ItemTypes.DIAMOND, 300.00));
        this.registerBaseMaterial(new BaseMaterial(ItemTypes.LEATHER, 0.80));
        // For ChainArmor
        this.registerBaseMaterial(new BaseMaterial(ItemTypes.IRON_BARS, 3.00));
    }

    public BaseMaterialContainer(Map<ItemType, Double> map)
    {
        for (Entry<ItemType, Double> entry : map.entrySet())
        {
            this.registerBaseMaterial(new BaseMaterial(entry.getKey(),entry.getValue()));
        }
    }

    public void registerBaseMaterial(BaseMaterial baseMaterial)
    {
        this.baseMaterials.put(baseMaterial.getMaterial(),baseMaterial);
    }

    public BaseMaterial of(ItemType mat)
    {
        return this.baseMaterials.get(mat);
    }

    public Map<ItemType, BaseMaterial> getBaseMaterials()
    {
        return baseMaterials;
    }
}
