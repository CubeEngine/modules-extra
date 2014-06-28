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
package de.cubeisland.engine.donations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.cubeisland.engine.core.command.CommandManager;
import de.cubeisland.engine.core.util.formatter.MessageType;
import de.cubeisland.engine.core.webapi.Action;
import de.cubeisland.engine.core.webapi.ApiRequest;
import de.cubeisland.engine.core.webapi.ApiResponse;
import de.cubeisland.engine.core.webapi.Method;
import de.cubeisland.engine.core.webapi.RequestMethod;
import de.cubeisland.engine.donations.DonationsConfig.DonationGoal;

public class DonationController
{
    private ObjectMapper mapper = new ObjectMapper();
    private Donations module;
    private DonationsConfig config;

    public DonationController(Donations module, DonationsConfig config)
    {
        this.module = module;
        this.config = config;
    }

    @Action
    @Method(RequestMethod.POST)
    public ApiResponse update(ApiRequest request)
    {
        JsonNode data = request.getData();

        // TODO get data from JsonNode!

        double newTotal = 0;

        this.updateDonation(newTotal);

        String user = "name";
        if (user != null)
        {
            this.broadcastDonation(user); // Automatic updated do not include a user
        }

        ApiResponse response = new ApiResponse();
        response.setContent(mapper.createObjectNode().put("response", "ok")); // TODO
        return response;
    }

    private void broadcastDonation(String user)
    {
        module.getCore().getUserManager().broadcastMessage(MessageType.POSITIVE, "New Donation! Thank you {user}!",
                                                           user);
    }

    private void updateDonation(double newTotal)
    {
        CommandManager cmdMan = module.getCore().getCommandManager();
        if (newTotal < this.config.lastTotal)
        {
            for (Double val : this.config.goals.keySet())
            {
                if (val > newTotal && val < config.lastTotal)
                {
                    DonationGoal goal = this.config.goals.get(val);
                    for (String cmd : goal.lost)
                    {
                        cmdMan.runCommand(cmdMan.getConsoleSender(), cmd);
                    }
                }
            }
        }
        else
        {
            for (Double val : this.config.goals.keySet())
            {
                if (val > config.lastTotal && val < newTotal)
                {
                    DonationGoal goal = this.config.goals.get(val);
                    for (String cmd : goal.reached)
                    {
                        cmdMan.runCommand(cmdMan.getConsoleSender(), cmd);
                    }
                }
            }
        }
    }
}
