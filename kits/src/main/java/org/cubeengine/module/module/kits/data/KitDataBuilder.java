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
package org.cubeengine.module.module.kits.data;

import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.DataManipulatorBuilder;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.data.value.ValueFactory;

import java.util.Optional;

public class KitDataBuilder extends AbstractDataBuilder<KitData> implements DataManipulatorBuilder<KitData, ImmutableKitData>
{
    private ValueFactory valueFactory;

    public KitDataBuilder(ValueFactory valueFactory)
    {
        super(KitData.class, 1);
        this.valueFactory = valueFactory;
    }

    @Override
    public KitData create()
    {
        return new KitData();
    }

    @Override
    public Optional<KitData> createFrom(DataHolder dataHolder)
    {
        return create().fill(dataHolder);
    }

    @Override
    protected Optional<KitData> buildContent(DataView container) throws InvalidDataException
    {
        return create().from(container.copy());
    }
}
