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
package org.cubeengine.module.selector;

import com.flowpowered.math.vector.Vector3d;
import org.cubeengine.libcube.util.math.MathHelper;
import org.cubeengine.libcube.util.math.shape.Cuboid;
import org.cubeengine.libcube.util.math.shape.Shape;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class SelectorData
{

    private Mode mode = Mode.CUBOID;

    private World lastPointWorld;

    private Location<World>[] points = new Location[mode.initialSize];

    public void setPoint(int index, Location<World> location)
    {
        this.points[index] = location;
        this.lastPointWorld = location.getExtent();
    }

    public int addPoint(Location location)
    {
        /*
        this.points.add(location);
        this.lastPointWorld = location.getWorld();
        return this.points.size();
        */
        throw new UnsupportedOperationException("Not supported yet!");
    }

    public Location getPoint(int index)
    {
        return this.points[index];
    }

    public Shape getSelection()
    {
        for (Location<World> point : this.points)
        {
            if (point == null) // missing point
            {
                return null;
            }
            if (lastPointWorld != point.getExtent()) // points are in different worlds
            {
                return null;
            }
        }
        if (this.getPoint(0) == null || this.getPoint(1) == null)
        {
            return null;
        }
        return this.getSelection0();
    }

    private Shape getSelection0()
    {
        Vector3d v1 = new Vector3d(this.getPoint(0).getX(), this.getPoint(0).getY(), this.getPoint(0).getZ());
        Vector3d v2 = new Vector3d(this.getPoint(1).getX(), this.getPoint(1).getY(), this.getPoint(1).getZ());
        return new Cuboid(v1, v2.sub(v1));
    }

    enum Mode
    {
        CUBOID(2);
        private int initialSize;

        Mode(int initialSize)
        {
            this.initialSize = initialSize;
        }

        public int initialSize()
        {
            return this.initialSize;
        }
    }
}
