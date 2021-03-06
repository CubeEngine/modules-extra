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
package org.cubeengine.module.shout.announce;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.cubeengine.reflect.annotations.Comment;
import org.cubeengine.reflect.annotations.Name;
import org.cubeengine.reflect.codec.yaml.ReflectedYaml;

@SuppressWarnings("all")
public class AnnouncementConfig extends ReflectedYaml
{
    @Comment("The delay a previous announcement and this one")
    public String delay = "10 minutes";

    @Comment("The name that should be used in the permission. It'll end up like this: " +
                 "cubeengine.shout.announcement.permission-name\n"
        + "Use * to skip the permission check")
    @Name("permission-name")
    public String permName = "*";

    @Comment("An announcement with fixed cycle will be broadcast at a fixed cycle.\n" +
                 "In opposite to it being displayed to each user after their last announcement.")
    @Name("fixed-cycle")
    public boolean fixedCycle = false;

    @Comment("The default announcment")
    public String announcement;

    @Comment("Higher weights than other messages cause this message to appear more often\n"
            + "e.g. an announcement with a weight of 2 is on average displays twice as much as an announcement with weight 1\n"
            + "but not 2 times in a row if there are more than one message")
    public int weight = 1;

    @Comment("The announcement for a locale\n"
        + "en_US: English_Version\n"
        + "de_DE: German_Version")
    public Map<Locale, String> translated = new HashMap<>();

    @Override
    public void onLoaded(File loadedFrom)
    {
        if (this.weight == 0)
        {
            this.weight = 1;
        }
    }
}
