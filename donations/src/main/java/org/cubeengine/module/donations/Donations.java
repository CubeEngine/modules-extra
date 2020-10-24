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
package org.cubeengine.module.donations;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.Broadcaster;
import org.cubeengine.libcube.service.filesystem.FileManager;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.module.apiserver.ApiServer;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Server;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;

import static org.cubeengine.module.apiserver.PluginApiServer.APISERVER_ID;
import static org.cubeengine.module.apiserver.PluginApiServer.APISERVER_VERSION;

@Singleton
@Module(dependencies = @Dependency(value = APISERVER_ID, version = APISERVER_VERSION))
public class Donations
{
    @Inject
    private FileManager fm;
    @Inject private TaskManager tm;
    @Inject private Broadcaster bc;
    @Inject private ModuleManager mm;

    @Listener
    public void onEnable(StartedEngineEvent<Server> event)
    {
        ApiServer apiServer = ((ApiServer) mm.getModule(ApiServer.class));
        DonationController controller = new DonationController(fm.loadConfig(this, DonationsConfig.class), tm, bc);
        apiServer.registerApiHandlers(Donations.class, controller);
    }
}
