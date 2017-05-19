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
package org.cubeengine.module.squelch.storage;

import java.util.UUID;
import org.cubeengine.libcube.service.database.AsyncRecord;

import static org.cubeengine.module.squelch.storage.TableIgnorelist.TABLE_IGNORE_LIST;

public class IgnoreList extends AsyncRecord<IgnoreList>
{
    public IgnoreList()
    {
        super(TABLE_IGNORE_LIST);
    }

    public IgnoreList newIgnore(UUID user, UUID ignore)
    {
        this.setValue(TABLE_IGNORE_LIST.ID, user);
        this.setValue(TABLE_IGNORE_LIST.ID, ignore);
        return this;
    }
}