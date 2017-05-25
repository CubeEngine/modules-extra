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
package org.cubeengine.module.vigil.data;

import java.util.Optional;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.DataManipulatorBuilder;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.InvalidDataException;

public class LookupDataBuilder extends AbstractDataBuilder<LookupData> implements DataManipulatorBuilder<LookupData, ImmutableLookupData>
{
    public LookupDataBuilder()
    {
        super(LookupData.class, 1);
    }

    @Override
    public LookupData create()
    {
        return new LookupData();
    }

    @Override
    public Optional<LookupData> createFrom(DataHolder dataHolder)
    {
        return create().fill(dataHolder);
    }

    @Override
    protected Optional<LookupData> buildContent(DataView container) throws InvalidDataException
    {
        return create().from(container.copy());
    }
}
