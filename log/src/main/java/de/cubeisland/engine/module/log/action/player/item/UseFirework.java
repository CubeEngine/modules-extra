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
package de.cubeisland.engine.module.log.action.player.item;

import de.cubeisland.engine.module.service.user.User;
import de.cubeisland.engine.module.log.LoggingConfiguration;
import de.cubeisland.engine.module.log.action.BaseAction;
import de.cubeisland.engine.module.log.action.player.ActionPlayer;
import org.spongepowered.api.text.Text;

import static de.cubeisland.engine.module.core.util.formatter.MessageType.POSITIVE;
import static de.cubeisland.engine.module.log.action.ActionCategory.USE;

/**
 * Represents a player launching a firework
 */
public class UseFirework extends ActionPlayer // TODO item
{
    public UseFirework()
    {
        super("firework", USE);
    }

    @Override
    public boolean canAttach(BaseAction action)
    {
        return action instanceof UseFirework && this.player.equals(((UseFirework)action).player);
    }

    @Override
    public Text translateAction(User user)
    {
        int count = this.countAttached();
        return user.getTranslationN(POSITIVE, count, "{user} launched a firework", "{user} launched {amount} fireworks",
                                    this.player.name, count);
    }

    @Override
    public boolean isActive(LoggingConfiguration config)
    {
        return config.use.firework;
    }
}
