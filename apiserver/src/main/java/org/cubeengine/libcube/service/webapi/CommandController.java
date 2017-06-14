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
package org.cubeengine.libcube.service.webapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.libcube.service.webapi.sender.ApiCommandSource;
import org.cubeengine.libcube.service.webapi.sender.ApiServerSender;
import org.cubeengine.libcube.service.webapi.sender.ApiUser;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;

public class CommandController
{
    private final ObjectMapper mapper = new ObjectMapper();
    private final TaskManager tm;
    private final CommandManager cm;
    private I18n i18n;

    public CommandController(I18n i18n, TaskManager tm, CommandManager cm)
    {
        this.tm = tm;
        this.cm = cm;
        this.i18n = i18n;
    }

    @Endpoint(route = "/value")
    public ApiResponse command(ApiRequest request, final @Value("cmd") String command)
    {
        User authUser = request.getAuthUser();
        final ApiCommandSource sender = authUser == null
                                        ? new ApiServerSender(request.getConnection(), mapper, Sponge.getServer().getConsole())
                                        : new ApiUser(request.getConnection(), mapper, authUser);

        cm.runCommand(sender, command);

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setContent(sender.flush());

        return apiResponse;
    }
}
