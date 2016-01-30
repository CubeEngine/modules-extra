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
package org.cubeengine.module.donations;

import javax.inject.Inject;
import de.cubeisland.engine.modularity.core.marker.Enable;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import org.cubeengine.service.filesystem.FileManager;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.task.TaskManager;
import org.cubeengine.service.user.Broadcaster;
import org.cubeengine.service.webapi.ApiServer;

@ModuleInfo(name = "Donations", description = "Provides WebAPI to handle donations")
public class Donations extends Module
{
    @Inject private FileManager fm;
    @Inject private CommandManager cm;
    @Inject private TaskManager tm;
    @Inject private Broadcaster bc;
    @Inject private ApiServer apiServer;

    @Enable
    public void onEnable()
    {
        DonationController controller = new DonationController(this, fm.loadConfig(this, DonationsConfig.class), cm, tm, bc);
        apiServer.registerApiHandlers(this, controller);
    }
}
