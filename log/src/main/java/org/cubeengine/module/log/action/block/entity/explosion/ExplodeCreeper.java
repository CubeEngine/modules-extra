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
package org.cubeengine.module.log.action.block.entity.explosion;

import org.cubeengine.service.user.User;
import org.cubeengine.module.log.LoggingConfiguration;
import org.cubeengine.module.log.action.BaseAction;
import org.spongepowered.api.text.Text;

import static org.cubeengine.service.i18n.formatter.MessageType.POSITIVE;

/**
 * Represents a creeper blowing up blocks
 */
public class ExplodeCreeper extends ExplosionAction
{
    public ExplodeCreeper()
    {
        super("creeper");
    }

    @Override
    public boolean canAttach(BaseAction action)
    {
        return action instanceof ExplodeCreeper && this.oldBlock.equals(((ExplodeCreeper)action).oldBlock)
            && this.entity.equals(((ExplodeCreeper)action).entity) && (
            (this.player == null && ((ExplodeCreeper)action).player == null) || (this.player != null
                && this.player.equals(((ExplodeCreeper)action).player)));
    }

    @Override
    public Text translateAction(User user)
    {
        int count = this.countAttached();
        if (this.player == null)
        {
            return user.getTranslationN(POSITIVE, count, "A Creeper-Explosion wrecked {name#block}",
                                        "A Creeper-Explosion wrecked {1:amount}x {name#block}", this.oldBlock.name(),
                                        count);
        }
        return user.getTranslationN(POSITIVE, count, "{user} let a Creeper detonate and destroy {name#block}",
                                    "{user} let a Creeper detonate and destroy {2:amount}x {name#block}",
                                    this.player.name, this.oldBlock.name(), count);
    }

    @Override
    public boolean isActive(LoggingConfiguration config)
    {
        return config.block.explode.creeper;
    }
}
