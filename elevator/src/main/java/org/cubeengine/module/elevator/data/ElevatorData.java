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
package org.cubeengine.module.elevator.data;

import com.flowpowered.math.vector.Vector3i;
import org.cubeengine.libcube.util.data.AbstractData;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.merge.MergeFunction;

import java.util.Optional;
import java.util.UUID;

public class ElevatorData extends AbstractData<ElevatorData, ImmutableElevatorData> implements IElevatorData
{
    private UUID owner;
    private Vector3i target;

    public ElevatorData()
    {
        super(1, Sign.class);
    }

    private ElevatorData with(UUID owner, Vector3i target)
    {
        this.owner = owner;
        this.target = target;
        return this;
    }

    public ElevatorData(IElevatorData data)
    {
        super(1);
        with(data.getOwner(), data.getTarget());
    }

    @Override
    protected void registerKeys()
    {
        registerSingle(OWNER, this::getOwner, this::setOwner);
        registerSingle(TARGET, this::getTarget, this::setTarget);
    }

    @Override
    public Vector3i getTarget()
    {
        return target;
    }

    public void setTarget(Vector3i target)
    {
        this.target = target;
    }

    @Override
    public UUID getOwner()
    {
        return owner;
    }

    public void setOwner(UUID owner)
    {
        this.owner = owner;
    }

    @Override
    public Optional<ElevatorData> fill(DataHolder dataholder, MergeFunction mergeFunction)
    {
        if (!supports(dataholder))
        {
            return Optional.empty();
        }
        ElevatorData merged = mergeFunction.merge(this,
            new ElevatorData().with(
                dataholder.get(OWNER).orElse(null),
                dataholder.get(TARGET).orElse(null)));
        if (merged != this)
        {
            this.with(merged.owner, merged.target);
        }
        return Optional.of(this);
    }

    @Override
    public Optional<ElevatorData> from(DataContainer container)
    {
        Optional<UUID> owner = container.getObject(OWNER.getQuery(), UUID.class);
        Optional<Vector3i> target = container.getObject(TARGET.getQuery(), Vector3i.class);

        if (owner.isPresent())
        {
            with(owner.orElse(null), target.orElse(null));
            return Optional.of(this);
        }
        return Optional.empty();
    }

    @Override
    public ElevatorData copy()
    {
        return new ElevatorData(this);
    }

    @Override
    public ImmutableElevatorData asImmutable()
    {
        return new ImmutableElevatorData(this);
    }

    @Override
    public ElevatorData asMutable()
    {
        return this;
    }
}
