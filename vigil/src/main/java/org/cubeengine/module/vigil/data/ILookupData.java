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
package org.cubeengine.module.vigil.data;

import static org.spongepowered.api.data.DataQuery.of;

import com.google.common.reflect.TypeToken;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.data.value.mutable.Value;

import java.util.List;
import java.util.UUID;

public interface ILookupData
{
    TypeToken<Value<UUID>> TTV_UUID = new TypeToken<Value<UUID>>() {};
    TypeToken<Value<Boolean>> TTV_Bool = new TypeToken<Value<Boolean>>() {};
    TypeToken<ListValue<String>> TTLV_String = new TypeToken<ListValue<String>>() {};

    Key<Value<UUID>> CREATOR = Key.builder().type(TTV_UUID).query(of("creator")).id("cubeengine-vigil:creator").name("Creator").build();
    Key<Value<Boolean>> FULLDATE = Key.builder().type(TTV_Bool).query(of("fulldate")).id("cubeengine-vigil:isfulldate").name("Full Date").build();
    Key<Value<Boolean>> SHOWLOC = Key.builder().type(TTV_Bool).query(of("showloc")).id("cubeengine-vigil:showloc").name("Show Location").build();
    Key<Value<Boolean>> NODATE = Key.builder().type(TTV_Bool).query(of("nodate")).id("cubeengine-vigil:nodate").name("No Date").build();
    Key<Value<Boolean>> FULLLOC = Key.builder().type(TTV_Bool).query(of("fullloc")).id("cubeengine-vigil:fullloc").name("Full Location").build();
    Key<Value<Boolean>> DETAILINV = Key.builder().type(TTV_Bool).query(of("detailinv")).id("cubeengine-vigil:detailinv").name("Detailed Inventory").build();
    Key<ListValue<String>> REPORTS = Key.builder().type(TTLV_String).query(of("reports")).id("cubeengine-vigil:reports").name("Filtered Reports").build();

    UUID getCreator();
    boolean isFullDate();
    boolean isShowLocation();
    boolean isNoDate();
    boolean isFullLocation();
    boolean showDetailedInventory();
    List<String> getReports();

    LookupData asMutable();

    List<String> filteredReports();
}
