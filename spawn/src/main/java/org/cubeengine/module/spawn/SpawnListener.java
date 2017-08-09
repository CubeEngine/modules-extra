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
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;

import static org.cubeengine.module.spawn.SpawnCommands.getSpawnLocation;

public class SpawnListener
{
    private PermissionService pm;

    public SpawnListener(PermissionService pm)
    {
        this.pm = pm;
    }

    public void onJoin(ClientConnectionEvent.Login event)
    {
        if (event.getTargetUser().get(Keys.LAST_DATE_PLAYED).isPresent())
        {
            return;
        }
        Subject subject = pm.getUserSubjects().getSubject(event.getTargetUser().getIdentifier()).get();
        Optional<String> option = subject.getOption(SpawnCommands.ROLESPAWN);
        if (option.isPresent())
        {
            event.setToTransform(getSpawnLocation(option.get()));
        }
    }

    @Listener(order = Order.LATE)
    public void onSpawn(RespawnPlayerEvent event)
    {
        if (!event.isBedSpawn())
        {
            Subject subject = pm.getUserSubjects().getSubject(event.getTargetEntity().getIdentifier()).get();
            Optional<String> option = subject.getOption(SpawnCommands.ROLESPAWN);
            if (option.isPresent())
            {
                event.setToTransform(getSpawnLocation(option.get()));
            }
        }
    }
}
