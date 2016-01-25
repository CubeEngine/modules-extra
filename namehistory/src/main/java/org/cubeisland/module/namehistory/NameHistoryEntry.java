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
package org.cubeisland.module.namehistory;

import java.sql.Date;
import de.cubeisland.engine.module.core.storage.database.AsyncRecord;
import org.cubeengine.service.user.User;
import org.cubeengine.module.core.util.McUUID.NameEntry;

import static org.cubeisland.module.namehistory.TableNameHistory.TABLE_NAMEHISTORY;

public class NameHistoryEntry extends AsyncRecord<NameHistoryEntry>
{
    public NameHistoryEntry()
    {
        super(TABLE_NAMEHISTORY);
    }

    public NameHistoryEntry newEntry(User user, NameEntry entry)
    {
        this.setValue(TABLE_NAMEHISTORY.NAME, entry.name);
        this.setValue(TABLE_NAMEHISTORY.USERID, user.getEntity().getId());
        this.setValue(TABLE_NAMEHISTORY.CHANGED_AT, new Date(entry.changedToAt));
        return this;
    }
}
