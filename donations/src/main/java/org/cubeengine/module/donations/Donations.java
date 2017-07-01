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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.service.filesystem.FileManager;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.libcube.service.Broadcaster;
import org.cubeengine.libcube.service.webapi.ApiServer;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;

@Singleton
@Module(id = "donations", name = "Donations", version = "1.0.0",
        description = "Provides WebAPI to handle donations",
        dependencies = {@Dependency("cubeengine-core"), @Dependency("cubeengine-apiserver")},
        url = "http://cubeengine.org",
        authors = {"Anselm 'Faithcaio' Brehme", "Phillip Schichtel"})
public class Donations extends CubeEngineModule
{
    @Inject private FileManager fm;
    @Inject private CommandManager cm;
    @Inject private TaskManager tm;
    @Inject private Broadcaster bc;
    @Inject private ApiServer apiServer;

    @Listener
    public void onEnable(GamePreInitializationEvent event)
    {
        DonationController controller = new DonationController(fm.loadConfig(this, DonationsConfig.class), cm, tm, bc);
        apiServer.registerApiHandlers(Donations.class, controller);
    }
}
