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
package org.cubeengine.module.elevator.data;

import java.util.UUID;
import io.leangen.geantyref.TypeToken;
import org.cubeengine.module.elevator.PluginElevator;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.block.entity.BlockEntity;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.persistence.DataStore;
import org.spongepowered.api.data.value.Value;
import org.spongepowered.api.event.lifecycle.RegisterCatalogEvent;
import org.spongepowered.math.vector.Vector3i;

public interface ElevatorData
{
    TypeToken<Value<UUID>> TTV_UUID = new TypeToken<Value<UUID>>() {};
    TypeToken<Value<Vector3i>> TTV_VECTOR = new TypeToken<Value<Vector3i>>() {};

    Key<Value<UUID>> OWNER = Key.builder().key(ResourceKey.of(PluginElevator.ELEVATOR_ID, "owner")).type(TTV_UUID).build();
    Key<Value<Vector3i>> TARGET = Key.builder().key(ResourceKey.of(PluginElevator.ELEVATOR_ID, "target")).type(TTV_VECTOR).build();


    static void register(RegisterCatalogEvent<DataRegistration> event)
    {
        final ResourceKey rkey = ResourceKey.of(PluginElevator.ELEVATOR_ID, "elevator");
        final DataStore dataStore = DataStore.builder().pluginData(rkey)
                                             .holder(BlockEntity.class)
                                             .key(OWNER, "owner")
                                             .key(TARGET, "target")
                                             .build();

        final DataRegistration registration = DataRegistration.builder()
                                                              .dataKey(OWNER, TARGET)
                                                              .store(dataStore)
                                                              .key(rkey)
                                                              .build();
        event.register(registration);
    }



}
