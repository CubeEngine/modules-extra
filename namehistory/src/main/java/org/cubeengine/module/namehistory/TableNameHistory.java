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
package org.cubeengine.module.namehistory;

import java.sql.Date;
import de.cubeisland.engine.module.core.storage.database.Table;
import de.cubeisland.engine.module.core.storage.database.TableUpdateCreator;
import org.cubeengine.module.core.util.Version;
import org.jooq.TableField;
import org.jooq.types.UInteger;

import org.cubeengine.service.user.TableUser.TABLE_USER;
import static org.jooq.impl.SQLDataType.DATE;
import static org.jooq.impl.SQLDataType.VARCHAR;

public class TableNameHistory extends Table<NameHistoryEntry>
{
    public static TableNameHistory TABLE_NAMEHISTORY;
    public final TableField<NameHistoryEntry, UInteger> USERID = createField("userId", U_INTEGER.nullable(false), this);
    public final TableField<NameHistoryEntry, String> NAME = createField("name", VARCHAR.length(50).nullable(false), this);
    public final TableField<NameHistoryEntry, Date> CHANGED_AT = createField("changedAt", DATE.nullable(false), this);

    public TableNameHistory(String prefix)
    {
        super(prefix + "namehistory", new Version(1));
        this.setPrimaryKey(USERID, CHANGED_AT);
        this.addForeignKey(TABLE_USER.getPrimaryKey(), USERID);
        this.addFields(USERID, NAME, CHANGED_AT);
        TABLE_NAMEHISTORY = this;
    }

    @Override
    public Class<NameHistoryEntry> getRecordType()
    {
        return NameHistoryEntry.class;
    }
}
