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
package org.cubeengine.module.donations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import de.cubeisland.engine.reflect.Section;
import de.cubeisland.engine.reflect.annotations.Comment;
import de.cubeisland.engine.reflect.codec.yaml.ReflectedYaml;

@SuppressWarnings("all")
public class DonationsConfig extends ReflectedYaml
{
    @Comment({"Example:", "10.0:", "  name: 10 ", "  reached:", "   - say reached {TOTAL} thanks to {NAME}", "lost:", "   - say lost {TOTAL}"})
    public Map<Double, DonationGoal> goals = new HashMap<>();
    public double lastTotal = 0;

    public List<String> forUser = new ArrayList<>();

    public static class DonationGoal implements Section
    {
        public String name;
        public List<String> reached = new ArrayList<>();
        public List<String> lost = new ArrayList<>();
    }
}
