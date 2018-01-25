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
package org.cubeengine.module.module.kits.data;

import static org.cubeengine.module.module.kits.data.KitData.TIME;
import static org.cubeengine.module.module.kits.data.KitData.TIMES;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.manipulator.ImmutableDataManipulator;
import org.spongepowered.api.data.value.BaseValue;
import org.spongepowered.api.data.value.immutable.ImmutableValue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ImmutableKitData implements ImmutableDataManipulator<ImmutableKitData, KitData>
{
    private final Map<String, Long> time;
    private final Map<String, Integer> times;

    public ImmutableKitData(Map<String, Long> time, Map<String, Integer> times)
    {
        this.time = time;
        this.times = times;
    }

    @Override
    public <E> Optional<ImmutableKitData> with(Key<? extends BaseValue<E>> key, E value)
    {
        if (TIME.equals(key))
        {
            return Optional.of(new ImmutableKitData((Map<String, Long>) value, times));
        }
        else if (TIMES.equals(key))
        {
            return Optional.of(new ImmutableKitData(time, (Map<String, Integer>) value));
        }
        return Optional.empty();
    }

    @Override
    public KitData asMutable()
    {
        return new KitData(time, times);
    }

    @Override
    public DataContainer toContainer()
    {
        DataContainer container = DataContainer.createNew();
        container.set(TIMES, this.times);
        container.set(TIME, this.time);
        return container;
    }

    @Override
    public <E> Optional<E> get(Key<? extends BaseValue<E>> key)
    {
        if (supports(key))
        {
            if (TIME.equals(key))
            {
                return Optional.of((E)time);
            }
            else if (TIMES.equals(key))
            {
                return Optional.of((E)times);
            }
        }
        return Optional.empty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E, V extends BaseValue<E>> Optional<V> getValue(Key<V> key)
    {
        if (supports(key))
        {
            if (TIME.equals(key))
            {
                return Optional.of((V) Sponge.getRegistry().getValueFactory().createMapValue(TIME, time));
            }
            else if (TIMES.equals(key))
            {
                return Optional.of((V) Sponge.getRegistry().getValueFactory().createMapValue(TIMES, times));
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean supports(Key<?> key)
    {
        return TIMES.equals(key) || TIME.equals(key);
    }

    @Override
    public Set<Key<?>> getKeys()
    {
        return new HashSet<>(Arrays.asList(TIMES, TIME));
    }

    @Override
    public Set<ImmutableValue<?>> getValues()
    {
        HashSet<ImmutableValue<?>> set = new HashSet<>();
        set.add(Sponge.getRegistry().getValueFactory().createMapValue(TIME, time).asImmutable());
        set.add(Sponge.getRegistry().getValueFactory().createMapValue(TIMES, times).asImmutable());
        return set;
    }

    @Override
    public int getContentVersion()
    {
        return 1;
    }
}
