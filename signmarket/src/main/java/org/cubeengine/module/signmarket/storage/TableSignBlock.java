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

import java.util.UUID;
import org.cubeengine.service.database.AutoIncrementTable;
import org.cubeengine.service.database.Database;
import org.cubeengine.module.core.util.Version;
import org.jooq.TableField;
import org.jooq.impl.SQLDataType;
import org.jooq.types.UInteger;
import org.jooq.types.UShort;

import static org.jooq.impl.SQLDataType.INTEGER;
import static org.jooq.impl.SQLDataType.TINYINT;

public class TableSignBlock extends AutoIncrementTable<BlockModel, UInteger>
{
    public static TableSignBlock TABLE_SIGN_BLOCK;
    public final TableField<BlockModel, UInteger> KEY = createField("key", U_INTEGER.nullable(false), this);
    public final TableField<BlockModel, UUID> WORLD = createField("world", SQLDataType.UUID.length(36).nullable(false), this);
    public final TableField<BlockModel, Integer> X = createField("x", INTEGER, this);
    public final TableField<BlockModel, Integer> Y = createField("y", INTEGER, this);
    public final TableField<BlockModel, Integer> Z = createField("z", INTEGER, this);
    public final TableField<BlockModel, Byte> SIGNTYPE = createField("signType", TINYINT, this);
    public final TableField<BlockModel, UUID> OWNER = createField("owner", SQLDataType.UUID.length(36), this);
    public final TableField<BlockModel, UInteger> ITEMKEY = createField("itemKey", U_INTEGER.nullable(false), this);
    public final TableField<BlockModel, UShort> AMOUNT = createField("amount", U_SMALLINT.nullable(false), this);
    public final TableField<BlockModel, UInteger> DEMAND = createField("demand", U_MEDIUMINT, this);
    public final TableField<BlockModel, UInteger> PRICE = createField("price", U_INTEGER.nullable(false), this);

    public TableSignBlock(String prefix, Database database)
    {
        super(prefix + "signmarketblocks", new Version(1), database);
        this.setAIKey(KEY);
        this.addIndex(WORLD, X, Y, Z);
        this.addForeignKey(TableSignItem.TABLE_SIGN_ITEM.getPrimaryKey(), ITEMKEY);
        this.addFields(KEY, WORLD, X, Y, Z, SIGNTYPE, OWNER, ITEMKEY, AMOUNT, DEMAND, PRICE);
        TABLE_SIGN_BLOCK = this;
    }

    @Override
    public Class<BlockModel> getRecordType()
    {
        return BlockModel.class;
    }
}
