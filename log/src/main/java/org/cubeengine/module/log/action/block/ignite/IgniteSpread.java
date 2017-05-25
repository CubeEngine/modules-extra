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
package org.cubeengine.module.log.action.block.ignite;

import org.cubeengine.module.log.LoggingConfiguration;
import org.cubeengine.module.log.action.BaseAction;
import org.cubeengine.libcube.service.user.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;

/**
 * Represents a fire spreading
 */
public class IgniteSpread extends ActionBlockIgnite
{
    public Coordinate source;

    public IgniteSpread()
    {
        super("spread");
    }

    @Override
    public boolean canAttach(BaseAction action)
    {
        return action instanceof IgniteSpread;
        // TODO attach if loc or source in common of any action attached
    }

    @Override
    public Text translateAction(User user)
    {
        int count = this.countAttached();
        return user.getTranslationN(POSITIVE, count, "Fire spread to this block", "Fire spread to {amount} blocks",
                                    count);
    }

    public void setSource(Location source)
    {
        this.source = new Coordinate(source);
    }

    @Override
    public boolean isActive(LoggingConfiguration config)
    {
        return config.ignite.spread;
    }
}
