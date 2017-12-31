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

import static org.spongepowered.api.data.DataQuery.of;
import static org.spongepowered.api.data.key.KeyFactory.makeMapKey;

import com.google.common.reflect.TypeToken;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.manipulator.mutable.common.AbstractData;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.value.mutable.MapValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class KitData extends AbstractData<KitData, ImmutableKitData>
{
    private static TypeToken<MapValue<String, Long>> TTMV_SL = new TypeToken<MapValue<String, Long>>() {};
    private static TypeToken<MapValue<String, Integer>> TTMV_SI = new TypeToken<MapValue<String, Integer>>() {};

    public static Key<MapValue<String, Long>> TIME = Key.builder().type(TTMV_SL).query(of("time_data")).id("cubeengine-kits:time_data").name("TimeData").build();
    public static Key<MapValue<String, Integer>> TIMES = Key.builder().type(TTMV_SI).query(of("times_data")).id("cubeengine-kits:times_data").name("TimesData").build();

    private Map<String, Long> time;
    private Map<String, Integer> times;

    public KitData(Map<String, Long> time, Map<String, Integer> times)
    {
        this.time = new HashMap<>();
        this.time.putAll(time);
        this.times = new HashMap<>();
        this.times.putAll(times);
        registerGettersAndSetters();
    }

    @Override
    protected void registerGettersAndSetters()
    {
        registerFieldGetter(TIME, KitData.this::getTime);
        registerFieldSetter(TIME, KitData.this::setTime);
        registerKeyValue(TIME, KitData.this::time);

        registerFieldGetter(TIMES, KitData.this::getTimes);
        registerFieldSetter(TIMES, KitData.this::setTimes);
        registerKeyValue(TIMES, KitData.this::times);
    }

    private void setTimes(Map<String, Integer> times)
    {
        this.times = times;
    }

    public MapValue<String, Integer> times()
    {
        return Sponge.getRegistry().getValueFactory().createMapValue(TIMES, this.times);
    }

    public Map<String, Integer>  getTimes()
    {
        return this.times;
    }

    public MapValue<String, Long> time()
    {
        return Sponge.getRegistry().getValueFactory().createMapValue(TIME, this.time);
    }

    public void setTime(Map<String, Long> time)
    {
        this.time = time;
    }

    public Map<String, Long> getTime()
    {
        return this.time;
    }

    public KitData()
    {
        this(new HashMap<>(), new HashMap<>());
    }

    @Override
    public Optional<KitData> fill(DataHolder dataHolder, MergeFunction overlap)
    {
        Optional<Map<String, Integer>> times = dataHolder.get(TIMES);
        Optional<Map<String, Long>> time = dataHolder.get(TIME);
        if (times.isPresent() || time.isPresent())
        {
            KitData data = this.copy();
            data.times = times.orElse(new HashMap<>());
            data.time = time.orElse(new HashMap<>());
            data = overlap.merge(this, data);
            if (data != this)
            {
                this.times = data.times;
                this.time = data.time;
            }
            return Optional.of(this);
        }
        return Optional.empty();
    }

    @Override
    public Optional<KitData> from(DataContainer container)
    {
        Optional<Map<String, Integer>> times = ((Optional<Map<String, Integer>>) container.getMap(TIMES.getQuery()));
        Optional<Map<String, Long>> time = ((Optional<Map<String, Long>>) container.getMap(TIME.getQuery()));
        if (times.isPresent() || time.isPresent())
        {
            this.times = times.get();
            this.time = time.get();
            return Optional.of(this);
        }
        return Optional.empty();
    }

    @Override
    public KitData copy()
    {
        return new KitData(this.time, this.times);
    }

    @Override
    public ImmutableKitData asImmutable()
    {
        return new ImmutableKitData(this.time, this.times);
    }

    @Override
    public DataContainer toContainer()
    {
        DataContainer container = super.toContainer();
        container.set(TIMES, this.times);
        container.set(TIME, this.time);
        return container;
    }

    @Override
    public int getContentVersion()
    {
        return 1;
    }
}
