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
package de.cubeisland.engine.module.log.action.vehicle;

import de.cubeisland.engine.service.user.User;
import de.cubeisland.engine.module.log.LoggingConfiguration;
import de.cubeisland.engine.module.log.action.BaseAction;
import org.spongepowered.api.text.Text;

import static de.cubeisland.engine.module.core.util.formatter.MessageType.POSITIVE;

/**
 * Represents a player breaking a vehicle
 */
public class VehicleBreak extends ActionVehicle
{
    // TODO actionType entity / block breakVechicle

    public VehicleBreak()
    {
        super("break");
    }

    @Override
    public boolean canAttach(BaseAction action)
    {
        return action instanceof VehicleBreak && this.player.equals(((VehicleBreak)action).player)
            && ((VehicleBreak)action).vehicle.isSameType(this.vehicle);
    }

    @Override
    public Text translateAction(User user)
    {
        int count = this.countAttached();
        return user.getTranslationN(POSITIVE, count, "{user} broke a {name#vehicle}",
                                    "{user} broke {2:amount} {name#vehicle}", this.player.name, this.vehicle.name(),
                                    count);
    }

    @Override
    public boolean isActive(LoggingConfiguration config)
    {
        return config.vehicle.destroy;
    }
}
