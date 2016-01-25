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
package org.cubeengine.module.log.action.player;

import org.cubeengine.module.log.action.BaseAction;
import org.cubeengine.service.user.User;
import org.cubeengine.module.log.LoggingConfiguration;
import org.spongepowered.api.text.Text;

import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.module.log.action.ActionCategory.PLAYER;

/**
 * Represents a player picking up an xp-orb
 */
public class PlayerXp extends ActionPlayer
{
    public int exp;

    public PlayerXp()
    {
        super("xp", PLAYER);
    }

    @Override
    public boolean canAttach(BaseAction action)
    {
        return action instanceof PlayerXp && this.player.equals(((PlayerXp)action).player);
    }

    @Override
    public Text translateAction(User user)
    {
        int amount = this.exp;
        if (this.hasAttached())
        {
            for (BaseAction action : this.getAttached())
            {
                amount += ((PlayerXp)action).exp;
            }
        }
        return user.getTranslation(POSITIVE, "{user} earned {amount} experience", this.player.name, amount);
    }

    public void setExp(int exp)
    {
        this.exp = exp;
    }

    @Override
    public boolean isActive(LoggingConfiguration config)
    {
        return config.player.xp;
    }
}
