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
package org.cubeengine.module.log.action.player.entity;

import org.cubeengine.module.log.LoggingConfiguration;
import org.cubeengine.module.log.action.ActionCategory;
import org.cubeengine.module.log.action.BaseAction;
import org.cubeengine.service.user.User;
import org.spongepowered.api.text.Text;

import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

/**
 * Represents a player shearing a sheep or mooshroom
 */
public class EntityShear extends ActionPlayerEntity
{
    public EntityShear()
    {
        super("shear", ActionCategory.ENTITY);
    }

    @Override
    public boolean canAttach(BaseAction action)
    {
        return action instanceof EntityShear && this.player.equals(((EntityShear)action).player)
            && ((EntityShear)action).entity.type == this.entity.type;
    }

    @Override
    public Text translateAction(User user)
    {
        int count = this.countAttached();
        return user.getTranslationN(POSITIVE, count, "{user} sheared {name#entity}",
                                    "{user} sheared {2:amount} {name#entity}", this.player.name, this.entity.name(),
                                    count);
    }

    @Override
    public boolean isActive(LoggingConfiguration config)
    {
        return config.entity.shear;
    }
}
