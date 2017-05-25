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
package org.cubeengine.module.log.action.block.player.interact;

import org.cubeengine.module.log.LoggingConfiguration;
import org.cubeengine.module.log.action.ActionCategory;
import org.cubeengine.module.log.action.BaseAction;
import org.cubeengine.module.log.action.block.player.ActionPlayerBlock;
import org.cubeengine.libcube.service.user.User;
import org.spongepowered.api.text.Text;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

/**
 * Represents a player accessing an {@link org.spongepowered.api.item.inventory.Carrier}
 */
public class UseContainer extends ActionPlayerBlock
{
    public UseContainer()
    {
        super("container", ActionCategory.USE);
    }

    @Override
    public boolean canAttach(BaseAction action)
    {
        return action instanceof UseContainer && this.player.equals(((ActionPlayerBlock)action).player)
            && this.coord.equals(action.coord);
    }

    @Override
    public Text translateAction(User user)
    {
        int count = this.countAttached();
        return user.getTranslationN(POSITIVE, count, "{user} looked into a {name#container}",
                                    "{user} looked into {2:amount} {name#container}", this.player.name,
                                    this.oldBlock.name(), count);
    }

    @Override
    public boolean isActive(LoggingConfiguration config)
    {
        return config.use.container;
    }

    @Override
    public boolean isStackable()
    {
        // TODO instead do not implement Rollbackable & Redoable
        return true;
    }
}
