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

import org.cubeengine.libcube.service.config.ConfigWorld;
import org.cubeengine.module.vigil.report.Report;
import org.cubeengine.reflect.annotations.Comment;
import org.cubeengine.reflect.codec.yaml.ReflectedYaml;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class VigilConfig extends ReflectedYaml
{
    @Comment({"Class names of disabled Reports for each world.",
              "org.cubeengine.module.vigil.report can be ommitted"})
    public Map<ConfigWorld, List<String>> disabledReports = new HashMap<>();

    private transient Map<UUID, List<Class<? extends Report>>> disabledReportsMap = new HashMap<>();

    public void markDirty(World world)
    {
        disabledReportsMap.remove(world.getUniqueId());
    }

    public List<Class<? extends Report>> getDisabledReports(World world)
    {
        List<Class<? extends Report>> reports = disabledReportsMap.get(world.getUniqueId());
        if (reports == null)
        {
            reports = new ArrayList<>();
            disabledReportsMap.put(world.getUniqueId(), reports);
            ConfigWorld cWorld = new ConfigWorld(world);
            for (String name : disabledReports.getOrDefault(cWorld, Collections.emptyList()))
            {
                if ("*".equals(name))
                {
                    name = "Report";
                }
                Report.getReport(name).ifPresent(reports::add);
            }
            if (!reports.isEmpty())
            {
                List<String> list = new ArrayList<>();
                disabledReports.put(cWorld, list);
                reports.forEach(c -> list.add(c.getClass().getName())); // Save long names
                this.save();
            }
        }
        return reports;
    }

}
