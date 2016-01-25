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
package org.cubeisland.module.signmarket.storage;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.cubeisland.module.signmarket.Signmarket;
import org.spongepowered.api.world.Location;
import org.jooq.DSLContext;
import org.jooq.types.UInteger;

import static org.cubeisland.module.signmarket.storage.TableSignBlock.TABLE_SIGN_BLOCK;

public class SignMarketBlockManager
{
    private Map<Location,BlockModel> blockModels;

    private final Signmarket module;
    private final DSLContext dsl;

    public SignMarketBlockManager(Signmarket module)
    {
        this.module = module;
        this.dsl = module.getCore().getDB().getDSL();
    }

    public void load()
    {
        this.blockModels = new HashMap<>();
        for (BlockModel model : this.dsl.selectFrom(TABLE_SIGN_BLOCK).fetch())
        {
            this.blockModels.put(model.getLocation(),model);
        }
        this.module.getLog().debug("{} block models loaded", this.blockModels.size());
    }

    public Collection<BlockModel> getLoadedModels()
    {
        return this.blockModels.values();
    }

    public void delete(BlockModel model)
    {
        this.blockModels.remove(model.getLocation());
        UInteger key = model.getValue(TABLE_SIGN_BLOCK.KEY);
        if (key == null || key.longValue() == 0) return; // unsaved model
        model.deleteAsync();
    }

    public void store(BlockModel blockModel)
    {
        this.blockModels.put(blockModel.getLocation(),blockModel);
        blockModel.insertAsync();
    }

    public void update(BlockModel blockItemModel)
    {
        blockItemModel.updateAsync();
    }
}
