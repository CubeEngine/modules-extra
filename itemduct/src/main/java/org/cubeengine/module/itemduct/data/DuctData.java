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
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.value.mutable.MapValue;
import org.spongepowered.api.data.value.mutable.Value;
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
    private Integer uses;

    public DuctData()
    {
        super(1);
    }

    public DuctData(IDuctData data)
    {
        this();
        setFilters(data.getFilters());
    }

    public DuctData(int uses)
    {
        this();
        this.uses = uses;
    }

    @Override
    protected void registerKeys()
    {
        registerGetter(FILTERS, this::getFilters);
        registerSetter(FILTERS, this::setFilters);
        registerValue(FILTERS, this::filters);

        registerGetter(USES, this::getUses);
        registerSetter(USES, this::setUses);
        registerValue(USES, this::uses);
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
    public int getUses()
    {
        return this.uses == null ? 0 : this.uses;
    }

    public DuctData setUses(Integer uses)
    {
        this.uses = uses;
        return this;
    }

    public Value<Integer> uses()
    {
        return Sponge.getRegistry().getValueFactory().createValue(USES, this.getUses());
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
        Optional<Integer> uses = dataHolder.get(USES);
        if (uses.isPresent())
        {
            DuctData data = this.copy();
            data.setUses(uses.get());
            data = overlap.merge(this, data);
            if (data != this)
            {
                this.setUses(data.getUses());
            }
            return Optional.of(this);
        }
        return Optional.empty();
    }

    @Override
    public Optional<DuctData> from(DataContainer container)
    {
        Optional<DataView> filters = container.getView(FILTERS.getQuery());
        if (filters.isPresent())
        {
            Map<Direction, List<ItemStack>> map = new HashMap<>();
            for (DataQuery key : filters.get().getKeys(false))
            {
                Direction dir = Direction.valueOf(key.toString());
                List<ItemStack> list = filters.get().getSerializableList(key, ItemStack.class).orElse(new ArrayList<>());
                map.put(dir, list);
            }
            this.setFilters((map));
            return Optional.of(this);
        }
        Optional<Integer> uses = container.getInt(USES.getQuery());
        if (uses.isPresent())
        {
            this.setUses(uses.get());
            return Optional.of(this);
        }
        return Optional.empty();
    }

    @Override
    public DuctData copy()
    {
        return new DuctData().setFilters(this.filters).setUses(this.uses);
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

    public boolean has(Direction dir)
    {
        return this.filters.containsKey(dir);
    }

    public void remove(Direction dir)
    {
        this.filters.remove(dir);
    }
}
