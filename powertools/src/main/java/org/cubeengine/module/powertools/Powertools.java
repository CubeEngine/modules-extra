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
package org.cubeengine.module.powertools;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.annotation.ModuleCommand;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.module.powertools.data.PowertoolData;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Server;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterCatalogEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;

@Singleton
@Module
public class Powertools
{
    @Inject private PowertoolsPerm perm;
    @Inject private EventManager em;

    @ModuleCommand PowertoolCommand powertoolCommand;

    @Listener
    public void onEnable(StartedEngineEvent<Server> event)
    {
        em.registerListener(Powertools.class, powertoolCommand);
    }

    @Listener
    public void onRegisterData(RegisterCatalogEvent<DataRegistration> event)
    {
        PowertoolData.register(event);
    }

    public PowertoolsPerm perms()
    {
        return perm;
    }
}
