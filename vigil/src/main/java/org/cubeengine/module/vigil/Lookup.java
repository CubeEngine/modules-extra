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
package org.cubeengine.module.vigil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bson.Document;
import org.cubeengine.module.vigil.data.LookupData;
import org.cubeengine.module.vigil.report.Report;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.server.ServerLocation;
import org.spongepowered.math.vector.Vector3i;

public class Lookup
{
    private Map<LookupTiming, Long> timingStart = new HashMap<>();
    private Map<LookupTiming, Long> timingTime = new HashMap<>();

    private LookupData settings;

    private ResourceKey world;
    private Vector3i position;
    private int radius = 0;
    private Document prepared;

    public Lookup(Document prepared)
    {
        this.settings = new LookupData();
        this.prepared = prepared;
    }

    public Lookup(LookupData settings)
    {
        this.settings = settings;
    }

    public Lookup with(ServerLocation loc)
    {
        this.world = loc.getWorld().getKey();
        this.position = loc.getBlockPosition();
        return this;
    }

    public Lookup withRadius(Integer radius)
    {
        if (radius != null)
        {
            this.radius = radius;
        }
        else
        {
            this.radius = 200;
        }
        return this;
    }

    public Lookup withReport(Report report)
    {
        if (report != null)
        {
            List<String> reports = this.settings.getReports();
            reports.add(report.getClass().getName());
        }
        return this;
    }

    public Lookup copy()
    {
        Lookup lookup = new Lookup(settings);
        lookup.world = this.world;
        lookup.position = this.position;
        lookup.settings = this.settings.copy();
        return lookup;
    }

    public ResourceKey getWorld()
    {
        return world;
    }

    public Vector3i getPosition()
    {
        return position;
    }

    public LookupData getSettings()
    {
        return settings;
    }

    public int getRadius()
    {
        return radius;
    }

    public void time(LookupTiming timing)
    {
        Long start = timingStart.remove(timing);
        if (start == null)
        {
            timingStart.put(timing, System.currentTimeMillis());
        }
        else
        {
            Long time = timingTime.getOrDefault(timing, 0L);
            time += System.currentTimeMillis() - start;
            timingTime.put(timing, time);
        }
    }

    public long timing(LookupTiming timing)
    {
        return timingTime.get(timing);
    }

    public Document prepared()
    {
        return this.prepared;
    }

    public enum LookupTiming
    {
        LOOKUP,
        REPORT
    }

}
