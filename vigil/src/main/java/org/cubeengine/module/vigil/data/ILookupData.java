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
import static org.spongepowered.api.data.key.KeyFactory.makeSingleKey;

import java.util.List;
import java.util.UUID;

import com.google.common.reflect.TypeToken;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.KeyFactory;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.data.value.mutable.Value;

public interface ILookupData
{

    TypeToken<UUID> TT_UUID = new TypeToken<UUID>() {};
    TypeToken<Value<UUID>> TTV_UUID = new TypeToken<Value<UUID>>() {};

    TypeToken<Boolean> TT_Bool = new TypeToken<Boolean>() {};
    TypeToken<Value<Boolean>> TTV_Bool = new TypeToken<Value<Boolean>>() {};

    TypeToken<List<String>> TTL_String = new TypeToken<List<String>>() {};
    TypeToken<ListValue<String>> TTLV_String = new TypeToken<ListValue<String>>() {};

    Key<Value<UUID>> CREATOR = makeSingleKey(TT_UUID, TTV_UUID, of("creator"), "cubeengine-vigil:creator", "Creator");
    Key<Value<Boolean>> FULLDATE = makeSingleKey(TT_Bool, TTV_Bool, of("fulldate"), "cubeengine-vigil:isfulldate", "Full Date");
    Key<Value<Boolean>> SHOWLOC = makeSingleKey(TT_Bool, TTV_Bool, of("showloc"), "cubeengine-vigil:showloc", "Show Location");
    Key<Value<Boolean>> NODATE = makeSingleKey(TT_Bool, TTV_Bool, of("nodate"), "cubeengine-vigil:nodate", "No Date");
    Key<Value<Boolean>> FULLLOC = makeSingleKey(TT_Bool, TTV_Bool, of("fullloc"), "cubeengine-vigil:fullloc", "Full Location");
    Key<Value<Boolean>> DETAILINV = makeSingleKey(TT_Bool, TTV_Bool, of("detailinv"), "cubeengine-vigil:detailinv", "Detailed Inventory");
    Key<ListValue<String>> REPORTS = makeSingleKey(TTL_String, TTLV_String, of("reports"), "cubeengine-vigil:reports", "Filtered Reports");

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
