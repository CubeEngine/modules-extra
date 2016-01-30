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
package org.cubeengine.module.holiday.storage;

import java.sql.Date;
import java.util.UUID;
import org.cubeengine.module.core.util.Version;
import org.cubeengine.service.database.Database;
import org.cubeengine.service.database.Table;
import org.jooq.TableField;
import org.jooq.impl.SQLDataType;

import static org.jooq.util.mysql.MySQLDataType.DATE;
import static org.jooq.util.mysql.MySQLDataType.VARCHAR;

public class TableHoliday extends Table<HolidayModel>
{
    public static TableHoliday TABLE_HOLIDAY;
    public final TableField<HolidayModel, UUID> USERID = createField("userid", SQLDataType.UUID.length(36).nullable(false), this);
    public final TableField<HolidayModel, Date> FROM = createField("from", DATE, this);
    public final TableField<HolidayModel, Date> TO = createField("to", DATE, this);
    public final TableField<HolidayModel, String> REASON = createField("reason", VARCHAR.length(255), this);

    public TableHoliday(String prefix, Database db)
    {
        super(prefix + "holiday", new Version(1), db);
        this.setPrimaryKey(USERID);
        this.addUniqueKey(USERID);
        this.addFields(USERID, FROM, TO, REASON);
        TABLE_HOLIDAY = this;
    }

    @Override
    public Class<HolidayModel> getRecordType()
    {
        return HolidayModel.class;
    }
}
