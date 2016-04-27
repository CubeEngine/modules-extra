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
package org.cubeengine.module.powertools.data;

import java.util.List;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.KeyFactory;
import org.spongepowered.api.data.value.mutable.ListValue;

public interface IPowertoolData
{
    Key<ListValue<String>> POWERS = KeyFactory.makeListKey(String.class, DataQuery.of("powers"));

    static int compare(IPowertoolData o2, IPowertoolData o1)
    {
        int compare = Integer.compare(o1.getPowers().size(), o2.getPowers().size());
        if (compare != 0)
        {
            return compare;
        }
        for (int i = 0; i < o1.getPowers().size(); i++)
        {
            compare = o1.getPowers().get(i).compareTo(o2.getPowers().get(i));
            if (compare != 0)
            {
                return compare;
            }
        }
        return 0;
    }

    List<String> getPowers();
}
