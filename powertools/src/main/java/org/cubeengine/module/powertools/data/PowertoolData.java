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
package org.cubeengine.module.powertools.data;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.manipulator.mutable.common.AbstractData;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.value.mutable.ListValue;

public class PowertoolData extends AbstractData<PowertoolData, ImmutablePowertoolData> implements IPowertoolData
{
    private List<String> powers;

    public PowertoolData()
    {
    }

    public PowertoolData(List<String> powers)
    {
        this.powers = powers;
        registerGettersAndSetters();
    }

    @Override
    protected void registerGettersAndSetters()
    {
        registerFieldGetter(POWERS, PowertoolData.this::getPowers);
        registerFieldSetter(POWERS, PowertoolData.this::setPowers);
        registerKeyValue(POWERS, PowertoolData.this::powers);
    }

    public ListValue<String> powers()
    {
        return Sponge.getRegistry().getValueFactory().createListValue(POWERS, powers);
    }

    public void setPowers(List<String> powers)
    {
        this.powers = powers;
    }

    public List<String> getPowers()
    {
        return Collections.unmodifiableList(powers);
    }

    @Override
    public Optional<PowertoolData> fill(DataHolder dataHolder, MergeFunction overlap)
    {
        Optional<List<String>> powers = dataHolder.get(POWERS);
        if (powers.isPresent())
        {
            PowertoolData data = this.copy();
            data.powers = powers.get();

            data = overlap.merge(this, data);

            if (data != this)
            {
                this.powers = data.powers;
            }
            return Optional.of(this);
        }
        return Optional.empty();
    }

    @Override
    public Optional<PowertoolData> from(DataContainer container)
    {
        Optional<List<String>> powers = container.getStringList(POWERS.getQuery());
        if (powers.isPresent())
        {
            this.powers = powers.get();
            return Optional.of(this);
        }
        return Optional.empty();
    }

    @Override
    public PowertoolData copy()
    {
        return new PowertoolData(powers);
    }

    @Override
    public ImmutablePowertoolData asImmutable()
    {
        return new ImmutablePowertoolData(powers);
    }

    @Override
    public int compareTo(PowertoolData o)
    {
        return IPowertoolData.compare(this, o);
    }

    @Override
    public DataContainer toContainer()
    {
        return super.toContainer().set(POWERS, this.powers);
    }

    @Override
    public int getContentVersion()
    {
        return 1;
    }
}
