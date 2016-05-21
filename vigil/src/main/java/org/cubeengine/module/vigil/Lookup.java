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
package org.cubeengine.module.vigil;

import java.util.UUID;
import com.flowpowered.math.vector.Vector3i;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class Lookup
{
    private boolean fullDate = false;
    private boolean noDate = false;
    private boolean showLocation = false;
    private boolean fullLocation = false;

    private UUID world;
    private Vector3i position;

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private Lookup internal = new Lookup();

        public Builder with(Location<World> loc)
        {
            internal.world = loc.getExtent().getUniqueId();
            internal.position = loc.getBlockPosition();
            return this;
        }

        public Lookup build()
        {
            return internal.copy();
        }
    }

    public Lookup copy()
    {
        Lookup lookup = new Lookup();
        lookup.world = this.world;
        lookup.position = this.position;
        return lookup;
    }

    public UUID getWorld()
    {
        return world;
    }

    public Vector3i getPosition()
    {
        return position;
    }

    public boolean isFullDate()
    {
        return fullDate;
    }

    public boolean isShowLocation()
    {
        return showLocation;
    }

    public boolean isNoDate()
    {
        return noDate;
    }

    public boolean isFullLocation()
    {
        return fullLocation;
    }
}
