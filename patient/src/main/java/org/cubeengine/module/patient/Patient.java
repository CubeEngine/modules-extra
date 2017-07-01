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
package org.cubeengine.module.patient;

import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;

import javax.inject.Singleton;

@Singleton
@Module(id = "patient", name = "Patient", version = "1.0.0",
        description = "Find out what your server is suffering from",
        dependencies = @Dependency("cubeengine-core"),
        url = "http://cubeengine.org",
        authors = {"Anselm 'Faithcaio' Brehme", "Phillip Schichtel"})
public class Patient extends CubeEngineModule
{
    @ModuleConfig private PatientConfig config;
    

    public void onEnable()
    {
        // TODO analyze stuff running on that server...
    }

    // TODO command to enable / disable
    // TODO command to enable SPAM mode (show data every few seconds)
}
