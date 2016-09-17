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
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.manipulator.immutable.common.AbstractImmutableData;
import org.spongepowered.api.data.value.immutable.ImmutableListValue;

public class ImmutablePowertoolData extends AbstractImmutableData<ImmutablePowertoolData, PowertoolData> implements IPowertoolData
{
    private List<String> powers;

    public ImmutablePowertoolData(List<String> powers)
    {
        this.powers = powers;
    }

    @Override
    public List<String> getPowers()
    {
        return Collections.unmodifiableList(powers);
    }

    public ImmutableListValue<String> powers()
    {
        return Sponge.getRegistry().getValueFactory().createListValue(POWERS, powers).asImmutable();
    }

    @Override
    protected void registerGetters()
    {
        registerFieldGetter(POWERS, this::getPowers);
        registerKeyValue(POWERS, this::powers);
    }

    @Override
    public PowertoolData asMutable()
    {
        return new PowertoolData(powers);
    }

    @Override
    public int getContentVersion()
    {
        return 1;
    }

    @Override
    public DataContainer toContainer()
    {
        return super.toContainer().set(PowertoolData.POWERS, powers);
    }
}
