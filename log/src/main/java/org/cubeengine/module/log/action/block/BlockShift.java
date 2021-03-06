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
package org.cubeengine.module.log.action.block;

import org.cubeengine.libcube.service.user.User;
import org.cubeengine.module.log.LoggingConfiguration;
import org.cubeengine.module.log.action.BaseAction;
import org.spongepowered.api.text.Text;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.module.log.action.ActionCategory.BLOCK;
import static org.bukkit.Material.AIR;
import static org.bukkit.Material.PISTON_EXTENSION;

/**
 * Represents blocks moved by pistons
 */
public class BlockShift extends ActionBlock
{
    private boolean push;
    // TODO piston main block?

    public BlockShift()
    {
        super("shift", BLOCK);
    }

    public boolean canAttach(BaseAction action)
    {
        return false;
    }

    @Override
    public Text translateAction(User user)
    {
        if (this.push)
        {
            if (this.newBlock.is(PISTON_EXTENSION))
            {
                if (this.oldBlock.is(AIR))
                {
                    return user.getTranslation(POSITIVE, "A piston extended");
                }
                return user.getTranslation(POSITIVE, "A piston extended pushing {name#block} away",
                                           this.oldBlock.name());
            }
            if (this.oldBlock.is(AIR))
            {
                return user.getTranslation(POSITIVE, "A piston pushed {name#block} into place", this.newBlock.name());
            }
            return user.getTranslation(POSITIVE, "A piston pushed {name#block} into the place of {name#block}",
                                       this.newBlock.name(), this.oldBlock.name());
        }
        if (this.newBlock.is(AIR))
        {
            if (this.oldBlock.is(PISTON_EXTENSION))
            {
                return user.getTranslation(POSITIVE, "A piston retracted");
            }
            return user.getTranslation(POSITIVE, "A piston retracted {name#block}", this.oldBlock.name());
        }
        return user.getTranslation(POSITIVE, "A piston retracted {name#block} to here", this.newBlock.name());
    }

    public void setRetract()
    {
        this.push = false;
    }

    public void setPush()
    {
        this.push = true;
    }

    @Override
    public boolean isActive(LoggingConfiguration config)
    {
        return config.block.shift;
    }
}
