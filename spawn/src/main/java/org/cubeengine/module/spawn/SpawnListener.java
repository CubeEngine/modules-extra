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
package org.cubeengine.module.spawn;

import java.util.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.living.player.RespawnPlayerEvent;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.math.vector.Vector3d;

@Singleton
public class SpawnListener
{
    private PermissionService pm;

    @Inject
    public SpawnListener(Spawn module)
    {
        this.pm = module.getPermissionService();
    }

    @Listener
    public void onJoin(ServerSideConnectionEvent.Login event)
    {
        if (event.user().get(Keys.LAST_DATE_PLAYED).isPresent())
        {
            return;
        }

        final Optional<Vector3d> pos = SpawnCommands.getSubjectSpawnPos(event.user());
        final Optional<Vector3d> rot = SpawnCommands.getSubjectSpawnRotation(event.user());
        final Optional<ServerWorld> world = SpawnCommands.getSubjectSpawnWorld(event.user());
        if (pos.isPresent() && rot.isPresent() && world.isPresent())
        {
            event.setToLocation(world.get().location(pos.get()));
            event.setToRotation(rot.get());
        }
    }

    @Listener(order = Order.LATE)
    public void onSpawn(RespawnPlayerEvent.Recreate event)
    {
        if (!event.isBedSpawn())
        {
            final Optional<Vector3d> pos = SpawnCommands.getSubjectSpawnPos(event.entity());
            final Optional<Vector3d> rot = SpawnCommands.getSubjectSpawnRotation(event.entity());
            final Optional<ServerWorld> world = SpawnCommands.getSubjectSpawnWorld(event.entity());
            if (pos.isPresent() && rot.isPresent() && world.isPresent())
            {
                event.setDestinationPosition(pos.get());
                event.recreatedPlayer().setRotation(rot.get());
            }
        }
    }
}
