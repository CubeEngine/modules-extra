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
package org.cubeengine.module.unbreakableboat.data;

import java.util.Optional;

import com.google.common.reflect.TypeToken;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.KeyFactory;
import org.spongepowered.api.data.manipulator.mutable.common.AbstractBooleanData;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.value.mutable.Value;

public class UnbreakableData extends AbstractBooleanData<UnbreakableData, ImmutableUnbreakableData>
{
    private static TypeToken<Boolean> TT_Bool = new TypeToken<Boolean>() {};
    private static TypeToken<Value<Boolean>> TTV_Bool = new TypeToken<Value<Boolean>>() {};

    public final static Key<Value<Boolean>> UNBREAKING = KeyFactory.makeSingleKey(TT_Bool, TTV_Bool, DataQuery.of("unbreakable"), "unbreakable", "Unbreakable");

    public UnbreakableData(Boolean value)
    {
        super(value, UNBREAKING, false);
    }

    @Override
    public ImmutableUnbreakableData asImmutable()
    {
        return new ImmutableUnbreakableData(getValue());
    }

    @Override
    public Optional<UnbreakableData> fill(DataHolder dataHolder, MergeFunction overlap)
    {
        Optional<Boolean> flymode = dataHolder.get(UNBREAKING);
        if (flymode.isPresent())
        {
            UnbreakableData data = new UnbreakableData(flymode.get());
            overlap.merge(this, data);
            if (data != this)
            {
                this.setValue(flymode.get());
            }
            return Optional.of(this);
        }
        return Optional.empty();
    }

    @Override
    public Optional<UnbreakableData> from(DataContainer container)
    {
        Optional<Boolean> flymode = container.getBoolean(UNBREAKING.getQuery());
        if (flymode.isPresent())
        {
            this.setValue(flymode.get());
            return Optional.of(this);
        }
        return Optional.empty();
    }

    @Override
    public UnbreakableData copy()
    {
        return new UnbreakableData(getValue());
    }

    @Override
    public int getContentVersion()
    {
        return 1;
    }
}
