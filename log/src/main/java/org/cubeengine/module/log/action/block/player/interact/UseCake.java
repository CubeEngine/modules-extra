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
package org.cubeengine.module.log.action.block.player.interact;

import org.cubeengine.module.log.LoggingConfiguration;
import org.cubeengine.module.log.action.BaseAction;
import org.cubeengine.module.log.action.block.player.ActionPlayerBlock;
import org.cubeengine.service.user.User;
import org.bukkit.material.Cake;
import org.spongepowered.api.text.Text;

import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.module.log.action.ActionCategory.USE;

/**
 * Represents a player eating a cake
 */
public class UseCake extends ActionPlayerBlock
{
    public UseCake()
    {
        super("cake", USE);
    }

    @Override
    public boolean canAttach(BaseAction action)
    {
        return false;
    }

    @Override
    public Text translateAction(User user)
    {
        int piecesLeft = this.newBlock.as(Cake.class).getSlicesRemaining();
        if (piecesLeft == 0)
        {
            return user.getTranslation(POSITIVE, "The cake is a lie! Ask {user} he knows it!", this.player.name);
        }
        else
        {
            return user.getTranslation(POSITIVE, "{user} ate a piece of cake", this.player.name);
        }
    }

    @Override
    public boolean isActive(LoggingConfiguration config)
    {
        return config.use.cake;
    }
}
