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
package de.cubeisland.engine.module.itemrepair.repair.storage;

import de.cubeisland.engine.core.storage.database.AsyncRecord;
import de.cubeisland.engine.core.world.WorldManager;
import org.bukkit.Location;
import org.bukkit.block.Block;

import static de.cubeisland.engine.module.itemrepair.repair.storage.TableRepairBlock.TABLE_REPAIR_BLOCK;

public class RepairBlockModel extends AsyncRecord<RepairBlockModel>
{
    public RepairBlockModel()
    {
        super(TABLE_REPAIR_BLOCK);
    }

    public RepairBlockModel newRepairBlock(Block block, WorldManager worldManager)
    {
        this.setValue(TABLE_REPAIR_BLOCK.WORLD, worldManager.getWorldId(block.getWorld()));
        this.setValue(TABLE_REPAIR_BLOCK.X, block.getX());
        this.setValue(TABLE_REPAIR_BLOCK.Y, block.getY());
        this.setValue(TABLE_REPAIR_BLOCK.Z, block.getZ());
        this.setValue(TABLE_REPAIR_BLOCK.TYPE, block.getType().name());
        return this;
    }

    public Block getBlock(WorldManager wm)
    {
        Location loc = new Location(wm.getWorld(this.getValue(TABLE_REPAIR_BLOCK.WORLD)),
                                    this.getValue(TABLE_REPAIR_BLOCK.X).doubleValue(),
                                    this.getValue(TABLE_REPAIR_BLOCK.Y).doubleValue(),
                                    this.getValue(TABLE_REPAIR_BLOCK.Z).doubleValue());
        return loc.getBlock();
    }
}
