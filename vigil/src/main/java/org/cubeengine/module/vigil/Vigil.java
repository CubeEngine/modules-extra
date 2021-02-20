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
package org.cubeengine.module.vigil;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.annotation.ModuleCommand;
import org.cubeengine.libcube.service.event.ModuleListener;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.module.vigil.commands.VigilCommands;
import org.cubeengine.module.vigil.data.VigilData;
import org.cubeengine.module.vigil.storage.QueryManager;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Server;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.LifecycleEvent;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;

import static org.cubeengine.module.bigdata.PluginBigdata.BIGDATA_ID;
import static org.cubeengine.module.bigdata.PluginBigdata.BIGDATA_VERSION;

@Singleton
@Module(dependencies = @Dependency(value = BIGDATA_ID, version = BIGDATA_VERSION))
public class Vigil
{
    @Inject private QueryManager qm;
    @ModuleConfig private VigilConfig config;
    @ModuleListener private ToolListener toolListener;
    @ModuleCommand private VigilCommands vigilCommands;

    @Listener
    public void onRegisterData(RegisterDataEvent event)
    {
        VigilData.register(event);
    }

    public VigilConfig getConfig()
    {
        return config;
    }

    public QueryManager getQueryManager()
    {
        return qm;
    }

    @Listener
    public void onShutdown(StoppingEngineEvent<Server> event)
    {
        this.qm.shutdown();
    }
}
