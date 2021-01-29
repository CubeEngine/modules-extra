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

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cubeengine.module.donations.DonationsConfig.DonationGoal;
import org.cubeengine.libcube.service.i18n.formatter.MessageType;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.libcube.service.Broadcaster;
import org.cubeengine.module.apiserver.Endpoint;
import org.cubeengine.module.apiserver.ApiRequest;
import org.cubeengine.module.apiserver.ApiResponse;
import org.cubeengine.module.apiserver.RequestMethod;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;

public class DonationController
{
    private ObjectMapper mapper = new ObjectMapper();
    private DonationsConfig config;
    private TaskManager tm;
    private Broadcaster bc;

    public DonationController(DonationsConfig config, TaskManager tm, Broadcaster bc)
    {
        this.config = config;
        this.tm = tm;
        this.bc = bc;
    }

    @Endpoint(route = "/update", method = RequestMethod.POST)
    public ApiResponse update(ApiRequest request)
    {
        JsonNode data = request.getData();
        JsonNode user = data.get("name");
        final double newTotal = data.get("total").asDouble();
        if (user != null)
        {
            final String userName = user.asText();
            this.broadcastDonation(userName);

            tm.runTask(Donations.class, () -> {
                for (String cmd : config.forUser)
                {
                    if (cmd.contains("{NAME}"))
                    {
                        if (userName == null)
                        {
                            continue;
                        }
                        cmd = cmd.replace("{NAME}", userName);
                    }
                    cmd = cmd.replace("{TOTAL}", String.format("%.2f", newTotal));
                    try
                    {
                        Sponge.getServer().getCommandManager().process(Sponge.getSystemSubject(), cmd);
                    }
                    catch (CommandException e)
                    {
                        throw new IllegalStateException(e);
                    }
                }
            });
        }

        this.updateDonation(newTotal, user == null ? null : user.asText());
        this.config.lastTotal = newTotal;
        this.config.save();

        ApiResponse response = new ApiResponse();
        response.setContent(mapper.createObjectNode().put("response", "ok")); // TODO
        return response;
    }

    private void broadcastDonation(String user)
    {
        bc.broadcastTranslated(MessageType.POSITIVE, "New Donation! Thank you {user}!", user);
    }

    private void updateDonation(final double newTotal, final String user)
    {
        final List<String> cmds = new ArrayList<>();
        if (newTotal < this.config.lastTotal)
        {
            for (Double val : this.config.goals.keySet())
            {
                if (val > newTotal && val <= config.lastTotal)
                {
                    DonationGoal goal = this.config.goals.get(val);
                    cmds.addAll(goal.lost);
                }
            }
        }
        else
        {
            for (Double val : this.config.goals.keySet())
            {
                if (val >= config.lastTotal && val < newTotal)
                {
                    DonationGoal goal = this.config.goals.get(val);
                    cmds.addAll(goal.reached);
                }
            }
        }
        tm.runTask(Donations.class, () -> {
            for (String cmd : cmds)
            {
                if (cmd.contains("{NAME}"))
                {
                    if (user == null)
                    {
                        continue;
                    }
                    cmd = cmd.replace("{NAME}", user);
                }
                cmd = cmd.replace("{TOTAL}", String.format("%.2f", newTotal));
                try
                {
                    Sponge.getServer().getCommandManager().process(Sponge.getSystemSubject(), cmd);
                }
                catch (CommandException e)
                {
                    throw new IllegalStateException(e);
                }
            }
        });


    }
}
