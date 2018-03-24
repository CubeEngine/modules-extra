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
package org.cubeengine.module.itemduct.data;

import org.cubeengine.libcube.util.data.AbstractImmutableData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.value.immutable.ImmutableMapValue;
import org.spongepowered.api.data.value.immutable.ImmutableValue;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.Direction;

import java.util.List;
import java.util.Map;

public class ImmutableDuctData extends AbstractImmutableData<ImmutableDuctData, DuctData> implements IDuctData
{
    private final Map<Direction, List<ItemStack>> filters;
    private final Integer uses;

    public ImmutableDuctData(IDuctData data)
    {
        this(data.getFilters(), null);
    }

    public ImmutableDuctData(Map<Direction, List<ItemStack>> filters, Integer uses)
    {
        super(1);
        this.filters = filters;
        this.uses = uses;
        registerGetters();
    }

    @Override
    protected void registerGetters()
    {
        registerGetter(FILTERS, this::getFilters);
        registerValue(FILTERS, this::filters);

        registerGetter(USES, this::getUses);
        registerValue(USES, this::uses);
    }

    private ImmutableMapValue<Direction, List<ItemStack>> filters()
    {
        return Sponge.getRegistry().getValueFactory().createMapValue(FILTERS, this.filters).asImmutable();
    }

    @Override
    public Map<Direction, List<ItemStack>> getFilters()
    {
        return this.filters;
    }


    @Override
    public int getUses()
    {
        return this.uses == null ? 0 : this.uses;
    }

    public ImmutableValue<Integer> uses()
    {
        return Sponge.getRegistry().getValueFactory().createValue(USES, this.getUses()).asImmutable();
    }

    @Override
    public DuctData asMutable()
    {
        return new DuctData(this);
    }
}
