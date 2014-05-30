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
package de.cubeisland.engine.module.signmarket.storage;

import java.util.HashMap;
import java.util.Set;

import de.cubeisland.engine.module.signmarket.Signmarket;
import org.jooq.DSLContext;
import org.jooq.types.UInteger;

import static de.cubeisland.engine.module.signmarket.storage.TableSignItem.TABLE_SIGN_ITEM;

public class SignMarketItemManager
{
    private final Signmarket module;
    private final DSLContext dsl;
    private HashMap<UInteger, ItemModel> itemInfoModels;

    public SignMarketItemManager(Signmarket module)
    {
        this.dsl = module.getCore().getDB().getDSL();
        this.module = module;
    }

    public void load()
    {
        this.itemInfoModels = new HashMap<>();
        for (ItemModel model : this.dsl.selectFrom(TABLE_SIGN_ITEM).fetch())
        {
            this.itemInfoModels.put(model.getValue(TABLE_SIGN_ITEM.KEY), model);
        }
        this.module.getLog().debug("{} item models loaded", this.itemInfoModels.size());
    }

    public ItemModel getInfoModel(UInteger key)
    {
        return this.itemInfoModels.get(key);
    }

    public void store(ItemModel itemInfo)
    {
        itemInfo.insert();
        this.itemInfoModels.put(itemInfo.getValue(TABLE_SIGN_ITEM.KEY), itemInfo);
    }

    public void deleteUnusedModels(Set<UInteger> usedKeys)
    {
        for (UInteger key : this.itemInfoModels.keySet())
        {
            if (!usedKeys.contains(key))
            {
                this.itemInfoModels.remove(key).delete();
                this.module.getLog().debug("deleted unused item model #{}", key);
            }
        }
    }

    public void delete(ItemModel itemInfo)
    {
        UInteger key = itemInfo.getValue(TABLE_SIGN_ITEM.KEY);
        if (key == null || key.longValue() == 0)
        {
            return; // unsaved model
        }
        this.itemInfoModels.remove(key).delete();
    }

    public void update(ItemModel itemInfo)
    {
        itemInfo.update();
    }
}
