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
package de.cubeisland.engine.module.chat.storage;

import java.sql.Date;
import org.cubeengine.module.core.util.Version;
import org.cubeengine.service.database.Database;
import org.cubeengine.service.database.Table;
import org.jooq.TableField;
import org.jooq.types.UInteger;

import static org.cubeengine.service.user.TableUser.TABLE_USER;
import static org.jooq.util.mysql.MySQLDataType.DATE;

public class TableMuted extends Table<Muted>
{
    public static TableMuted TABLE_MUTED;
    public final TableField<Muted, UInteger> ID = createField("id", U_INTEGER.nullable(false), this);
    public final TableField<Muted, Date> MUTED = createField("muted", DATE, this);

    public TableMuted(String prefix, Database db)
    {
        super(prefix + "mute", new Version(1), db);
        setPrimaryKey(ID);
        addForeignKey(TABLE_USER.getPrimaryKey(), ID);
        addFields(ID, MUTED);
        TABLE_MUTED = this;
    }

    @Override
    public Class<Muted> getRecordType()
    {
        return Muted.class;
    }
}
