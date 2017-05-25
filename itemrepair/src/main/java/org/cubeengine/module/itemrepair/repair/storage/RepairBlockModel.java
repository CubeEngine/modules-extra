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
package org.cubeengine.module.itemrepair.repair.storage;

import com.flowpowered.math.vector.Vector3i;
import com.sun.org.apache.bcel.internal.generic.RET;
import org.cubeengine.libcube.service.database.AsyncRecord;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.module.itemrepair.repair.storage.TableRepairBlock.TABLE_REPAIR_BLOCK;

public class RepairBlockModel extends AsyncRecord<RepairBlockModel>
{
    public RepairBlockModel()
    {
        super(TABLE_REPAIR_BLOCK);
    }

    public RepairBlockModel newRepairBlock(Location<World> block)
    {
        this.setValue(TABLE_REPAIR_BLOCK.WORLD, block.getExtent().getUniqueId());
        Vector3i pos = block.getBlockPosition();
        this.setValue(TABLE_REPAIR_BLOCK.X, pos.getX());
        this.setValue(TABLE_REPAIR_BLOCK.Y, pos.getY());
        this.setValue(TABLE_REPAIR_BLOCK.Z, pos.getZ());
        this.setValue(TABLE_REPAIR_BLOCK.TYPE, block.getBlockType().getName());
        return this;
    }

    public Location<World> getBlock()
    {
        return new Location<>(Sponge.getServer().getWorld(getValue(TABLE_REPAIR_BLOCK.WORLD)).get(), this.getValue(TABLE_REPAIR_BLOCK.X), this.getValue(TABLE_REPAIR_BLOCK.Y), this.getValue(TABLE_REPAIR_BLOCK.Z));
    }
}
