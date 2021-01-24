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
package org.cubeengine.module.terra;

import com.google.inject.Inject;
import org.cubeengine.libcube.service.command.DispatcherCommand;
import org.cubeengine.libcube.service.command.annotation.Command;
import org.spongepowered.api.command.CommandCause;

@Command(name = "terra", desc = "Terra Commands")
public class TerraCommands extends DispatcherCommand
{
    private final TerraListener listener;

    @Inject
    public TerraCommands(TerraListener listener)
    {
        this.listener = listener;
    }

    @Command(desc = "Shows Terra status")
    public void status(CommandCause cause)
    {
        this.listener.printStatus(cause.getAudience());
    }
}
