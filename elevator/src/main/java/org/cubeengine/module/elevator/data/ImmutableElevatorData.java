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
import org.cubeengine.libcube.util.data.AbstractImmutableData;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.item.inventory.ItemStack;

import java.util.UUID;

public class ImmutableElevatorData extends AbstractImmutableData<ImmutableElevatorData, ElevatorData> implements IElevatorData
{
    private final UUID owner;
    private final Vector3i target;

    public ImmutableElevatorData(IElevatorData data)
    {
        super(1);
        this.owner = data.getOwner();
        this.target = data.getTarget();
    }

    @Override
    protected void registerGetters()
    {
        registerSingle(OWNER, this::getOwner);
        registerSingle(TARGET, this::getTarget);
    }

    @Override
    public UUID getOwner()
    {
        return owner;
    }

    @Override
    public Vector3i getTarget()
    {
        return target;
    }

    @Override
    public ElevatorData asMutable()
    {
        return new ElevatorData(this);
    }

    @Override
    public int getContentVersion()
    {
        return 1;
    }

    @Override
    public DataContainer toContainer()
    {
        return super.toContainer();
    }
}
