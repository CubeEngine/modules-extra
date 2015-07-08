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
package de.cubeisland.engine.module.log.action.block.flow;

import de.cubeisland.engine.service.user.User;
import de.cubeisland.engine.module.log.LoggingConfiguration;
import de.cubeisland.engine.module.log.action.BaseAction;
import org.spongepowered.api.text.Text;

import static de.cubeisland.engine.service.i18n.formatter.MessageType.POSITIVE;
import static de.cubeisland.engine.module.log.action.ActionCategory.WATER;

/**
 * Represents water flowing
 */
public class WaterFlow extends ActionFlow
{
    public WaterFlow()
    {
        super("flow", WATER);
    }

    @Override
    public boolean canAttach(BaseAction action)
    {
        return false;
    }

    @Override
    public Text translateAction(User user)
    {
        int count = this.countAttached();
        return user.getTranslationN(POSITIVE, count, "Water flooded this block",
                                    "Water flooded this block {amount} times", count);
    }

    @Override
    public boolean isActive(LoggingConfiguration config)
    {
        return config.block.flow.water;
    }
}
