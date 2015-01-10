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
package de.cubeisland.engine.module.log.action.block;

import de.cubeisland.engine.core.user.User;
import de.cubeisland.engine.module.log.LoggingConfiguration;
import de.cubeisland.engine.module.log.action.BaseAction;
import de.cubeisland.engine.module.log.action.ReferenceHolder;
import de.cubeisland.engine.module.log.action.block.player.ActionPlayerBlock;
import de.cubeisland.engine.reflect.codec.mongo.Reference;

import static de.cubeisland.engine.core.util.formatter.MessageType.POSITIVE;
import static de.cubeisland.engine.module.log.action.ActionCategory.BLOCK;

/**
 * Represents blocks falling
 */
public class BlockFall extends ActionBlock implements ReferenceHolder
{
    public Reference<ActionPlayerBlock> cause;

    public BlockFall()
    {
        super("fall", BLOCK);
    }

    @Override
    public boolean canAttach(BaseAction action)
    {
        return false;
    }

    @Override
    public String translateAction(User user)
    {
        // TODO plurals
        if (this.cause == null)
        {
            return user.getTranslation(POSITIVE, "{name#block} did fall to a lower place", this.oldBlock.name());
        }
        return user.getTranslation(POSITIVE, "{name#block} did fall to a lower place because of {user}",
                                   this.oldBlock.name(), cause.fetch(ActionPlayerBlock.class).player.name);
    }

    @Override
    public boolean isActive(LoggingConfiguration config)
    {
        return config.block.fall;
    }
}