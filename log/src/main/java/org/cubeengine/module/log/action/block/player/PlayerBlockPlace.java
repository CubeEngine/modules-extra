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

import org.cubeengine.module.log.action.block.player.bucket.BucketLava;
import org.cubeengine.module.log.action.block.player.bucket.BucketWater;
import org.cubeengine.service.user.User;
import org.cubeengine.module.log.LoggingConfiguration;
import org.cubeengine.module.log.action.ActionCategory;
import org.cubeengine.module.log.action.BaseAction;
import org.spongepowered.api.text.Text;

import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.module.log.action.ActionCategory.BLOCK;
import static org.bukkit.Material.AIR;
import static org.spongepowered.api.block.BlockTypes.AIR;

/**
 * Represents a player placing a block
 * <p>SubActions:
 * {@link BucketLava}
 * {@link BucketWater}
 */
public class PlayerBlockPlace extends ActionPlayerBlock
{
    public PlayerBlockPlace()
    {
        super("place", BLOCK);
    }

    public PlayerBlockPlace(String name, ActionCategory... categories)
    {
        super(name, categories);
    }

    @Override
    public boolean canAttach(BaseAction action)
    {
        return action instanceof PlayerBlockPlace && this.player.equals(((PlayerBlockPlace)action).player)
            && ((PlayerBlockPlace)action).oldBlock.material == this.oldBlock.material
            && ((PlayerBlockPlace)action).newBlock.material == this.newBlock.material;
    }

    @Override
    public Text translateAction(User user)
    {
        if (this.hasAttached())
        {
            int amount = this.getAttached().size() + 1;
            if (this.oldBlock.is(AIR))
            {
                return user.getTranslation(POSITIVE, "{user} placed {amount}x {name#block}",
                                           this.player.name, amount, this.newBlock.name());
            }
            return user.getTranslation(POSITIVE, "{user} replaced {amount}x {name#block} with {name#block}",
                                       this.player.name, amount, this.oldBlock.name(), this.newBlock.name());
        }
        // else single
        if (this.oldBlock.is(AIR))
        {
            return user.getTranslation(POSITIVE, "{user} placed {name#block}", this.player.name,
                                       this.newBlock.name());
        }
        return user.getTranslation(POSITIVE, "{user} replaced {name#block} with {name#block}",
                                   this.player.name, this.oldBlock.name(), this.newBlock.name());
    }

    @Override
    public boolean isActive(LoggingConfiguration config)
    {
        return config.block.placeByPlayer;
    }
}
