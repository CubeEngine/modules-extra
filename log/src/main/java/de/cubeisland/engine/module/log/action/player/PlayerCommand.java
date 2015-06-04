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
package de.cubeisland.engine.module.log.action.player;

import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.log.LoggingConfiguration;
import de.cubeisland.engine.module.log.action.BaseAction;

import de.cubeisland.engine.module.core.util.formatter.MessageType.POSITIVE;
import static de.cubeisland.engine.module.log.action.ActionCategory.PLAYER;

/**
 * Represents a player executing a command
 */
public class PlayerCommand extends ActionPlayer
{
    public String command;

    public PlayerCommand()
    {
        super("command", PLAYER);
    }

    @Override
    public boolean canAttach(BaseAction action)
    {
        return action instanceof PlayerCommand && this.player.equals(((PlayerCommand)action).player)
            && ((PlayerCommand)action).command.equalsIgnoreCase(this.command);
    }

    @Override
    public String translateAction(User user)
    {
        int count = this.countAttached();
        return user.getTranslationN(POSITIVE, count, "{user} used the command \"{input#command}\"",
                                    "{user} used the command \"{input#command}\" x{amount}",
                                    this.player.name, this.command, count);
    }

    public void setCommand(String command)
    {
        this.command = command;
    }

    @Override
    public boolean isActive(LoggingConfiguration config)
    {
        return config.player.command_enable;
    }
}
