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
package org.cubeengine.module.elevator;

import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.elevator.data.ElevatorData;
import org.cubeengine.module.elevator.data.ElevatorDataBuilder;
import org.cubeengine.module.elevator.data.ImmutableElevatorData;
import org.spongepowered.api.Sponge;

import javax.inject.Inject;

@ModuleInfo(name = "Elevator", description = "Lift up and down using signs")
public class Elevator extends Module
{
    @ModuleConfig private ElevatorConfig config;
    @Inject private EventManager em;
    @Inject private I18n i18n;

    @Enable
    public void onEnable()
    {
        Sponge.getDataManager().register(ElevatorData.class, ImmutableElevatorData.class, new ElevatorDataBuilder());
        em.registerListener(Elevator.class, new ElevatorListener(i18n, config));
    }
}
