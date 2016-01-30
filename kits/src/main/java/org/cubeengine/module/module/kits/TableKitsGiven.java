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
package org.cubeengine.module.module.kits;

import java.util.UUID;
import org.cubeengine.module.core.util.Version;
import org.cubeengine.service.database.Database;
import org.cubeengine.service.database.Table;
import org.jooq.TableField;
import org.jooq.impl.SQLDataType;

import static org.jooq.impl.SQLDataType.INTEGER;
import static org.jooq.impl.SQLDataType.VARCHAR;

public class TableKitsGiven extends Table<KitsGiven>
{
    public static TableKitsGiven TABLE_KITS;
    public final TableField<KitsGiven, UUID> USERID = createField("userId", SQLDataType.UUID.length(36).nullable(false),
                                                                  this);
    public final TableField<KitsGiven, String> KITNAME = createField("kitName", VARCHAR.length(50).nullable(false),
                                                                     this);
    public final TableField<KitsGiven, Integer> AMOUNT = createField("amount", INTEGER.nullable(false), this);

    public TableKitsGiven(String prefix, Database db)
    {
        super(prefix + "kits", new Version(1,1), db);
        this.setPrimaryKey(USERID, KITNAME);
        this.addFields(USERID, KITNAME, AMOUNT);
        TABLE_KITS = this;
    }

    @Override
    public Class<KitsGiven> getRecordType()
    {
        return KitsGiven.class;
    }

}
