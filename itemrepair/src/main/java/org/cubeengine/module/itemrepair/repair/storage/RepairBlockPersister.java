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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.cubeengine.module.sql.database.Database;
import org.cubeengine.module.itemrepair.Itemrepair;
import org.jooq.DSLContext;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import static org.cubeengine.module.itemrepair.repair.storage.TableRepairBlock.TABLE_REPAIR_BLOCK;

public class RepairBlockPersister
{
    private final Map<Location<World>,RepairBlockModel> models = new HashMap<>();
    private final Itemrepair module;
    private Database db;

    public RepairBlockPersister(Itemrepair module, Database db)
    {
        this.module = module;
        this.db = db;
    }

    public void deleteByBlock(Location<World> block)
    {
        RepairBlockModel repairBlockModel = this.models.remove(block);
        if (repairBlockModel != null)
        {
            repairBlockModel.deleteAsync();
        }
        else
        {
            this.module.getLog().warn("Could not delete model by block!");
        }
    }

    public Collection<RepairBlockModel> getAll(World world)
    {
        Collection <RepairBlockModel> all = this.db.getDSL().selectFrom(TABLE_REPAIR_BLOCK).where(TABLE_REPAIR_BLOCK.WORLD.eq(world.getUniqueId())).fetch();
        for (RepairBlockModel repairBlockModel : all)
        {
            this.models.put(repairBlockModel.getBlock(),repairBlockModel);
        }
        return all;
    }

    public void storeBlock(Location<World> block, RepairBlockModel repairBlockModel)
    {
        repairBlockModel.insertAsync();
        this.models.put(block,repairBlockModel);
    }
}
