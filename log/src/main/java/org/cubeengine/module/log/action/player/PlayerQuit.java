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
package org.cubeengine.module.log.action.player;

import org.cubeengine.libcube.service.user.User;
import org.cubeengine.module.log.LoggingConfiguration;
import org.cubeengine.module.log.action.BaseAction;
import org.spongepowered.api.text.Text;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.module.log.action.ActionCategory.PLAYER;

/**
 * Represents a Player leaving the server
 */
public class PlayerQuit extends ActionPlayer
{
    public String reason;

    public PlayerQuit()
    {
        super("quit", PLAYER);
    }

    @Override
    public boolean canAttach(BaseAction action)
    {
        return action instanceof PlayerQuit && this.player.equals(((PlayerQuit)action).player);
    }

    @Override
    public Text translateAction(User user)
    {
        if (this.hasAttached())
        {
            return user.getTranslation(POSITIVE, "{user} left the server x{amount}", this.player.name,
                                       this.getAttached().size() + 1);
        }
        return user.getTranslation(POSITIVE, "{user} left the server", this.player.name);
        // TODO reason
    }

    public void setReason(String reason)
    {
        this.reason = reason;
    }

    @Override
    public boolean isActive(LoggingConfiguration config)
    {
        return config.player.quit;
    }
}
