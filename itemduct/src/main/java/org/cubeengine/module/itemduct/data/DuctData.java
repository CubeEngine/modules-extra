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

import org.cubeengine.libcube.util.data.AbstractData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.value.mutable.MapValue;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.Direction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DuctData extends AbstractData<DuctData, ImmutableDuctData> implements IDuctData
{
    private Map<Direction, List<ItemStack>> filters = new HashMap<>();

    public DuctData()
    {
        super(1);
    }

    public DuctData(IDuctData data)
    {
        this();
        setFilters(data.getFilters());
    }

    @Override
    protected void registerKeys()
    {
        registerGetter(FILTERS, this::getFilters);
        registerSetter(FILTERS, this::setFilters);
        registerValue(FILTERS, this::filters);
    }

    private MapValue<Direction, List<ItemStack>> filters()
    {
        return Sponge.getRegistry().getValueFactory().createMapValue(FILTERS, this.filters);
    }

    private DuctData setFilters(Map<Direction, List<ItemStack>> filters)
    {
        this.filters = new HashMap<>(filters);
        return this;
    }

    @Override
    public Map<Direction, List<ItemStack>> getFilters()
    {
        return this.filters;
    }

    @Override
    public Optional<DuctData> fill(DataHolder dataHolder, MergeFunction overlap)
    {
        Optional<Map<Direction, List<ItemStack>>> filters = dataHolder.get(FILTERS);
        if (filters.isPresent())
        {
            DuctData data = this.copy();
            data.setFilters(filters.get());
            data = overlap.merge(this, data);
            if (data != this)
            {
                this.setFilters(data.getFilters());
            }
            return Optional.of(this);
        }
        return Optional.empty();
    }

    @Override
    public Optional<DuctData> from(DataContainer container)
    {
        Optional<? extends Map<?, ?>> filters = container.getMap(FILTERS.getQuery());
        if (filters.isPresent())
        {
            this.setFilters(((Map<Direction, List<ItemStack>>) filters.get()));
            return Optional.of(this);
        }
        return Optional.empty();
    }

    @Override
    public DuctData copy()
    {
        return new DuctData().setFilters(this.filters);
    }

    @Override
    public ImmutableDuctData asImmutable()
    {
        return new ImmutableDuctData(this);
    }

    public DuctData with(Direction dir)
    {
        this.filters.computeIfAbsent(dir, d -> new ArrayList<>());
        return this;
    }

    public Optional<List<ItemStack>> get(Direction dir)
    {
        return Optional.ofNullable(this.filters.get(dir));
    }
}
