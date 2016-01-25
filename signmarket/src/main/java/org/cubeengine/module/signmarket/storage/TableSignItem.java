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
package org.cubeengine.module.signmarket.storage;

import org.cubeengine.service.database.AutoIncrementTable;
import org.cubeengine.service.database.Database;
import de.cubeisland.engine.module.core.storage.database.AutoIncrementTable;
import org.cubeengine.module.core.util.Version;
import org.jooq.TableField;
import org.jooq.impl.SQLDataType;
import org.jooq.types.UInteger;
import org.jooq.types.UShort;

import static org.jooq.impl.SQLDataType.VARCHAR;

public class TableSignItem extends AutoIncrementTable<ItemModel, UInteger>
{
    public static TableSignItem TABLE_SIGN_ITEM;
    public final TableField<ItemModel, UInteger> KEY = createField("key", U_INTEGER.nullable(false), this);
    public final TableField<ItemModel, UInteger> STOCK = createField("stock", U_MEDIUMINT, this);
    public final TableField<ItemModel, String> ITEM = createField("item", VARCHAR.length(32).nullable(false), this);
    public final TableField<ItemModel, UShort> DAMAGEVALUE = createField("damageValue", U_SMALLINT.nullable(false), this);
    public final TableField<ItemModel, String> CUSTOMNAME = createField("customName",VARCHAR.length(100),this);
    public final TableField<ItemModel, String> LORE = createField("lore", VARCHAR.length(1000),this);
    public final TableField<ItemModel, String> ENCHANTMENTS = createField("enchantments",VARCHAR.length(255),this);
    public final TableField<ItemModel, Byte> SIZE = createField("size", SQLDataType.TINYINT.nullable(false),this);

    public TableSignItem(String prefix, Database database)
    {
        super(prefix + "signmarketitem", new Version(1), database);
        this.setAIKey(KEY);
        this.addFields(KEY, STOCK, ITEM, DAMAGEVALUE, CUSTOMNAME, LORE, ENCHANTMENTS, SIZE);
        TABLE_SIGN_ITEM = this;
    }

    @Override
    public Class<ItemModel> getRecordType()
    {
        return ItemModel.class;
    }
}
