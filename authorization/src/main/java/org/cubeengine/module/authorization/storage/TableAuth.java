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
package org.cubeengine.module.authorization.storage;

import java.util.UUID;
import org.cubeengine.libcube.util.Version;
import org.cubeengine.libcube.service.database.Database;
import org.cubeengine.libcube.service.database.Table;
import org.jooq.TableField;
import org.jooq.impl.SQLDataType;

import static org.jooq.impl.SQLDataType.VARBINARY;

public class TableAuth extends Table<Auth>
{
    public static TableAuth TABLE_AUTH;
    public final TableField<Auth, UUID> ID = createField("key", SQLDataType.UUID.length(36).nullable(false), this);
    public final TableField<Auth, byte[]> PASSWD = createField("passwd", VARBINARY.length(128), this);

    public TableAuth(String prefix, Database db)
    {
        super(prefix + "auth", new Version(1), db);
        setPrimaryKey(ID);
        addFields(ID, PASSWD);
        TABLE_AUTH = this;
    }

    @Override
    public Class<Auth> getRecordType()
    {
        return Auth.class;
    }
}
