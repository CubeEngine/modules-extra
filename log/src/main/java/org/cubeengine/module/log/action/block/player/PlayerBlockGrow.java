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
package org.cubeengine.module.log.action.block.player;

import org.cubeengine.module.log.LoggingConfiguration;
import org.cubeengine.module.log.action.BaseAction;
import org.cubeengine.service.user.User;
import org.spongepowered.api.text.Text;

import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.module.log.action.ActionCategory.BLOCK;
import static org.bukkit.Material.AIR;
import static org.spongepowered.api.block.BlockTypes.AIR;

/**
 * Represents a player letting a tree or mushroom grow using bonemeal
 */
public class PlayerBlockGrow extends ActionPlayerBlock
{
    public PlayerBlockGrow()
    {
        super("grow", BLOCK);
    }

    @Override
    public boolean canAttach(BaseAction action)
    {
        return action instanceof PlayerBlockGrow && this.player.equals(((PlayerBlockGrow)action).player)
            && ((PlayerBlockGrow)action).oldBlock == this.oldBlock
            && ((PlayerBlockGrow)action).newBlock == this.newBlock;
    }

    @Override
    public Text translateAction(User user)
    {
        int count = this.countAttached();
        if (this.oldBlock.is(AIR))
        {
            return user.getTranslationN(POSITIVE, count, "{user} let grow {name#block}",
                                        "{user} let grow {2:amount}x {name#block}", this.player.name,
                                        this.newBlock.name(), count);
        }
        return user.getTranslationN(POSITIVE, count, "{user} let grow {name#block} into {name#block}",
                                    "{user} let grow {3:amount}x {name#block} into {name#block}", this.player.name,
                                    this.newBlock.name(), this.oldBlock.name(), count);
    }

    @Override
    public boolean isActive(LoggingConfiguration config)
    {
        return config.block.growByPlayer;
    }
}
