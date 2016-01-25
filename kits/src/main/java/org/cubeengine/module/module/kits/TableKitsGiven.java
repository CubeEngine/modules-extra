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

import java.sql.Connection;
import java.sql.SQLException;
import de.cubeisland.engine.module.core.storage.database.Table;
import de.cubeisland.engine.module.core.storage.database.TableUpdateCreator;
import org.cubeengine.module.core.util.Version;
import org.jooq.TableField;
import org.jooq.types.UInteger;

import org.cubeengine.service.user.TableUser.TABLE_USER;
import static org.jooq.impl.SQLDataType.INTEGER;
import static org.jooq.impl.SQLDataType.VARCHAR;

public class TableKitsGiven extends Table<KitsGiven> implements TableUpdateCreator<KitsGiven>
{
    public static TableKitsGiven TABLE_KITS;
    public final TableField<KitsGiven, UInteger> USERID = createField("userId", U_INTEGER.nullable(false), this);
    public final TableField<KitsGiven, String> KITNAME = createField("kitName", VARCHAR.length(50).nullable(false),
                                                                     this);
    public final TableField<KitsGiven, Integer> AMOUNT = createField("amount", INTEGER.nullable(false), this);

    public TableKitsGiven(String prefix)
    {
        super(prefix + "kits", new Version(1,1));
        this.setPrimaryKey(USERID, KITNAME);
        this.addForeignKey(TABLE_USER.getPrimaryKey(), USERID);
        this.addFields(USERID, KITNAME, AMOUNT);
        TABLE_KITS = this;
    }

    @Override
    public Class<KitsGiven> getRecordType()
    {
        return KitsGiven.class;
    }

    @Override
    public void update(Connection connection, Version dbVersion) throws SQLException
    {
        if (dbVersion.getMajor() == 1 && dbVersion.getMinor() == 0)
        {
            connection.prepareStatement("ALTER TABLE cube_kits DROP PRIMARY KEY, ADD PRIMARY KEY (userId, kitName)").execute();
        }
    }
}
