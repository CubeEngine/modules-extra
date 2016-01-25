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
package org.cubeengine.module.log.action.death;

import org.cubeengine.module.log.action.player.item.ItemDrop;
import org.cubeengine.service.user.User;
import org.cubeengine.module.log.LoggingConfiguration;
import org.cubeengine.module.log.action.BaseAction;
import org.cubeengine.module.log.action.ReferenceHolder;
import de.cubeisland.engine.reflect.codec.mongo.Reference;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;

import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.module.log.action.ActionCategory.DEATH;

/**
 * Represents a player dropping items on death
 */
public class PlayerDeathDrop extends BaseAction implements ReferenceHolder
{
    public Reference<DeathPlayer> death;
    public ItemStack item;

    public PlayerDeathDrop()
    {
        super("drop", DEATH);
    }

    @Override
    public boolean canAttach(BaseAction action)
    {
        return action instanceof PlayerDeathDrop && this.death != null && ((PlayerDeathDrop)action).death != null
            && this.death.equals(((PlayerDeathDrop)action).death);
    }

    @Override
    public Text translateAction(User user)
    {
        int amount = this.item.getQuantity();
        if (this.hasAttached())
        {
            for (BaseAction action : this.getAttached())
            {
                amount += ((ItemDrop)action).item.getQuantity();
            }
        }
        return user.getTranslation(POSITIVE, "{user} dropped {name#item} x{amount} upon death", this.death.fetch(
            DeathPlayer.class).killed.name, this.item.getItem().name(), amount);
    }

    @Override
    public boolean isActive(LoggingConfiguration config)
    {
        return config.item.drop_onPlayerDeath;
    }
}
