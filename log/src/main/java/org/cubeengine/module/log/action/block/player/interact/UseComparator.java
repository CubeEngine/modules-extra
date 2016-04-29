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
import org.cubeengine.libcube.service.user.User;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.text.Text;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.module.log.action.ActionCategory.USE;

/**
 * Represents a player changing a comparator state
 */
public class UseComparator extends ActionPlayerBlock
{
    public UseComparator()
    {
        super("comparator", USE);
    }

    @Override
    public boolean canAttach(BaseAction action)
    {
        return action instanceof UseComparator && this.player.equals(((ActionPlayerBlock)action).player)
            && this.coord.equals(action.coord);
    }

    @Override
    public Text translateAction(User user)
    {
        if (this.hasAttached())
        {
            return user.getTranslation(POSITIVE, "{user} changed the comparator state {amount} times", this.player.name,
                                       this.countAttached());
        }
        if (this.newBlock.is(BlockTypes.POWERED_COMPARATOR))
        {
            return user.getTranslation(POSITIVE, "{user} activated the comparator", this.player.name);
        }
        return user.getTranslation(POSITIVE, "{user} deactivated the comparator", this.player.name);
    }

    @Override
    public boolean isActive(LoggingConfiguration config)
    {
        return config.use.comparator;
    }
}
