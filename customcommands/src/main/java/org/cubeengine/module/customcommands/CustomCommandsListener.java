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
package org.cubeengine.module.customcommands;

import java.util.ArrayList;
import java.util.List;
import org.cubeengine.libcube.util.StringUtils;
import org.cubeengine.libcube.service.Broadcaster;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;

import static java.util.Locale.ENGLISH;
import static org.spongepowered.api.text.format.TextFormat.NONE;

public class CustomCommandsListener
{
    private final Customcommands customcommands;
    private Broadcaster bc;
    private final CustomCommandsConfig config;

    public CustomCommandsListener(Customcommands customcommands, Broadcaster bc)
    {
        this.customcommands = customcommands;
        this.bc = bc;
        this.config = this.customcommands.getConfig();
    }

    @Listener
    public void onChat(SendCommandEvent event)
    {
        handleMessages(event.getCommand(), event);
    }

    @Listener
    public void onChat(MessageChannelEvent.Chat event)
    {
        handleMessages(event.getRawMessage().toPlain(), event);
    }

    private void handleMessages(String message, Event event)
    {
        List<String> messages = processMessage(message);
        for (String currMessage : messages)
        {
            bc.broadcastMessage(NONE, currMessage);
        }
        if (config.surpressMessage && event instanceof Cancellable)
        {
            ((Cancellable)event).setCancelled(true);
        }
    }

    private List<String> processMessage(String message)
    {
        String[] commands;
        List<String> messages = new ArrayList<>();

        if (message.contains("!"))
        {
            commands = StringUtils.explode("!", message.substring(message.indexOf("!")), false);

            for (String command : commands)
            {
                command = command.toLowerCase(ENGLISH);
                int indexOfSpace = command.indexOf(" ");

                if (indexOfSpace > -1)
                {
                    command = command.substring(0, indexOfSpace);
                }

                command = config.commands.get(command);
                if (command == null || "".equals(command))
                {
                    continue;
                }

                messages.add(command);
            }
        }
        return messages;
    }
}
