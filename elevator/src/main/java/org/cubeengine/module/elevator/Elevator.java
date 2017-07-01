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
package org.cubeengine.module.elevator;

import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.elevator.data.ElevatorData;
import org.cubeengine.module.elevator.data.ElevatorDataBuilder;
import org.cubeengine.module.elevator.data.ImmutableElevatorData;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.plugin.PluginContainer;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Module
public class Elevator extends CubeEngineModule
{
    @ModuleConfig private ElevatorConfig config;
    @Inject private ElevatorPerm perm;
    @Inject private EventManager em;
    @Inject private I18n i18n;
    @Inject PluginContainer plugin;

    @Listener
    public void onEnable(GamePreInitializationEvent event)
    {
        DataRegistration.<ElevatorData, ImmutableElevatorData>builder()
                .dataClass(ElevatorData.class).immutableClass(ImmutableElevatorData.class)
                .builder(new ElevatorDataBuilder()).manipulatorId("elevator")
                .dataName("CubeEngine Elevator Data")
                .buildAndRegister(plugin);

        em.registerListener(Elevator.class, new ElevatorListener(i18n, this));
    }

    public ElevatorPerm getPerm()
    {
        return perm;
    }

    public ElevatorConfig getConfig() {
        return config;
    }
}
