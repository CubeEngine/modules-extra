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
package org.cubeengine.module.log.action.player;

import org.cubeengine.module.log.LoggingConfiguration;
import org.cubeengine.module.log.action.BaseAction;
import org.cubeengine.libcube.service.user.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.POSITIVE;
import static org.cubeengine.module.log.action.ActionCategory.PLAYER;

/**
 * Represents a player teleport from one location to an other
 */
public class PlayerTeleport extends ActionPlayer
{
    public Coordinate toCoord;
    public boolean fromToDirection;

    public PlayerTeleport()
    {
        super("teleport", PLAYER);
    }

    @Override
    public boolean canAttach(BaseAction action)
    {
        return action instanceof PlayerTeleport && this.player.equals(((PlayerTeleport)action).player)
            && !this.hasAttached() && ((PlayerTeleport)action).fromToDirection != this.fromToDirection
            && action.coord.equals(this.toCoord) && this.coord.equals(((PlayerTeleport)action).toCoord);
    }

    @Override
    public Text translateAction(User user)
    {
        if (this.hasAttached())
        {
            Coordinate from = fromToDirection ? this.coord : this.toCoord;
            Coordinate to = fromToDirection ? this.toCoord : this.coord;
            return user.getTranslation(POSITIVE, "{user} teleported from {vector} in {world} to {vector} in {world}",
                                       this.player.name, to.toBlockVector(), to.getWorld(), from.toBlockVector(),
                                       from.getWorld());
        }
        if (this.fromToDirection)
        {
            return user.getTranslation(POSITIVE, "{user} teleported to {vector} in {world}", this.player.name,
                                       this.toCoord.toBlockVector(), this.toCoord.getWorld());
        }
        return user.getTranslation(POSITIVE, "{user} teleported here from {vector} in {world}", this.player.name,
                                   this.coord.toBlockVector(), this.coord.getWorld());
    }

    public void setOtherLocation(Location otherLocation, boolean fromTo)
    {
        this.toCoord = new Coordinate(otherLocation);
        this.fromToDirection = fromTo;
    }

    @Override
    public boolean isActive(LoggingConfiguration config)
    {
        return config.player.teleport;
    }
}
