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
package org.cubeengine.module.log.action.block.player.worldedit;

import org.cubeengine.module.log.LoggingConfiguration;
import org.cubeengine.module.log.action.BaseAction;
import org.cubeengine.module.log.action.block.player.ActionPlayerBlock;
import org.cubeengine.libcube.service.user.User;
import org.spongepowered.api.text.Text;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.module.log.action.ActionCategory.BLOCK;
import static org.bukkit.Material.AIR;
import static org.spongepowered.api.block.BlockTypes.AIR;

/**
 * Represents a player changing blocks using the worldedit plugin
 */
public class ActionWorldEdit extends ActionPlayerBlock
{
    public ActionWorldEdit()
    {
        super("worldedit", BLOCK);
    }

    @Override
    public boolean canAttach(BaseAction action)
    {
        return action instanceof ActionWorldEdit && this.player.equals(((ActionWorldEdit)action).player)
            && this.newBlock.material == ((ActionWorldEdit)action).newBlock.material
            && this.oldBlock.material == ((ActionWorldEdit)action).oldBlock.material;
    }

    @Override
    public Text translateAction(User user)
    {
        int count = this.countAttached();
        if (this.newBlock.is(AIR))
        {
            return user.getTranslationN(POSITIVE, count, "{user} used worldedit to remove {name#block}",
                                        "{user} used worldedit to remove {name#block} x{amount}", this.player.name,
                                        this.oldBlock.name(), count);
        }
        else if (this.oldBlock.is(AIR))
        {
            return user.getTranslationN(POSITIVE, count, "{user} used worldedit to place {name#block}",
                                        "{user} used worldedit to place {name#block} x{amount}", this.player.name,
                                        this.newBlock.name(), count);
        }
        else
        {
            return user.getTranslationN(POSITIVE, count,
                                        "{user} used worldedit to replace {name#block} with {name#block}",
                                        "{user} used worldedit to replace {name#block} with {name#block} x{amount}",
                                        this.player.name, this.oldBlock.name(), this.newBlock.name(), count);
        }
    }

    @Override
    public boolean isActive(LoggingConfiguration config)
    {
        return config.block.worldedit;
    }
}
