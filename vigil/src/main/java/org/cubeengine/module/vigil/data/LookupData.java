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
package org.cubeengine.module.vigil.data;

import java.util.Optional;
import java.util.UUID;
import org.cubeengine.libcube.util.data.AbstractData;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.item.inventory.ItemStack;

public class LookupData extends AbstractData<LookupData, ImmutableLookupData> implements ILookupData
{
    private UUID creator;

    public LookupData()
    {
        super(1, ItemStack.class);
    }

    public LookupData with(UUID uuid)
    {
        this.creator = uuid;
        return this;
    }

    @Override
    protected void registerKeys()
    {
        registerSingle(ILookupData.CREATOR, this::getCreator, this::setCreator);
    }

    @Override
    public UUID getCreator()
    {
        return creator;
    }

    public void setCreator(UUID uuid)
    {
        this.creator = uuid;
    }

    @Override
    public LookupData asMutable()
    {
        return this;
    }

    @Override
    public Optional<LookupData> fill(DataHolder dataHolder, MergeFunction overlap)
    {
        if (!supports(dataHolder))
        {
            return Optional.empty();
        }
        LookupData merged = overlap.merge(this, new LookupData().with(dataHolder.get(CREATOR).get()));
        if (merged != this)
        {
            this.with(merged.creator);
        }
        return Optional.of(this);
    }

    @Override
    public Optional<LookupData> from(DataContainer container)
    {
        Optional<UUID> creator = container.getObject(CREATOR.getQuery(), UUID.class);
        if (creator.isPresent())
        {
            with(creator.get());
            return Optional.of(this);
        }
        return Optional.empty();
    }

    @Override
    public ImmutableLookupData asImmutable()
    {
        return new ImmutableLookupData(this);
    }

    @Override
    public LookupData copy()
    {
        return new LookupData().with(creator);
    }
}
