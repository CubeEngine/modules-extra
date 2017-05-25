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

import java.util.UUID;
import org.cubeengine.libcube.service.database.AutoIncrementTable;
import org.cubeengine.libcube.service.database.Database;
import org.cubeengine.libcube.util.Version;
import org.jooq.TableField;
import org.jooq.impl.SQLDataType;
import org.jooq.types.UInteger;

import static org.jooq.impl.SQLDataType.INTEGER;
import static org.jooq.impl.SQLDataType.VARCHAR;

public class TableRepairBlock extends AutoIncrementTable<RepairBlockModel, UInteger>
{
    public static TableRepairBlock TABLE_REPAIR_BLOCK;
    public final TableField<RepairBlockModel, UInteger> ID = createField("id", U_INTEGER.nullable(false), this);
    public final TableField<RepairBlockModel, UUID> WORLD = createField("world", SQLDataType.UUID.length(36).nullable(false), this);
    public final TableField<RepairBlockModel, Integer> X = createField("x", INTEGER.nullable(false), this);
    public final TableField<RepairBlockModel, Integer> Y = createField("y", INTEGER.nullable(false), this);
    public final TableField<RepairBlockModel, Integer> Z = createField("z", INTEGER.nullable(false), this);
    public final TableField<RepairBlockModel, String> TYPE = createField("type", VARCHAR.length(64).nullable(false), this);

    public TableRepairBlock(String prefix, Database database)
    {
        super(prefix + "itemrepair_blocks", new Version(1), database);
        this.setAIKey(ID);
        this.addUniqueKey(WORLD, X, Y, Z);
        this.addFields(ID, WORLD, X, Y, Z, TYPE);
        TABLE_REPAIR_BLOCK = this;
    }

    @Override
    public Class<RepairBlockModel> getRecordType()
    {
        return RepairBlockModel.class;
    }
}
