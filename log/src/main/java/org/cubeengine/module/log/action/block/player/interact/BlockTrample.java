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

import org.cubeengine.libcube.service.user.User;
import org.cubeengine.module.log.LoggingConfiguration;
import org.cubeengine.module.log.action.BaseAction;
import org.cubeengine.module.log.action.block.player.ActionPlayerBlock;
import org.spongepowered.api.text.Text;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.module.log.action.ActionCategory.BLOCK;
import static org.spongepowered.api.block.BlockTypes.FARMLAND;

/**
 * Represents a player trampling crops
 */
public class BlockTrample extends ActionPlayerBlock
{
    public BlockTrample()
    {
        super("trample", BLOCK);
    }

    @Override
    public boolean canAttach(BaseAction action)
    {
        return action instanceof BlockTrample && !this.hasAttached() && this.player.equals(
            ((ActionPlayerBlock)action).player) && 50 > Math.abs(this.date.getTime() - action.date.getTime())
            && this.coord.worldUUID.equals(action.coord.worldUUID) && Math.abs(
            this.coord.vector.y - action.coord.vector.y) == 1 && this.coord.vector.x == action.coord.vector.x
            && this.coord.vector.z == action.coord.vector.z;
    }

    @Override
    public Text translateAction(User user)
    {
        BlockTrample action = this;
        if (this.hasAttached())
        {
            if (this.oldBlock.is(FARMLAND))
            {
                // replacing SOIL log with the crop log as the destroyed SOIL is implied
                action = (BlockTrample)this.getAttached().get(0);
            }
        }
        return user.getTranslation(POSITIVE, "{user} trampeled down {name#block}", action.player.name,
                                   action.oldBlock.name());
    }

    @Override
    public boolean isActive(LoggingConfiguration config)
    {
        return config.block.trample;
    }
}
