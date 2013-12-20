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
package de.cubeisland.engine.worlds.config;

import de.cubeisland.engine.configuration.YamlConfiguration;
import de.cubeisland.engine.configuration.annotations.Comment;

public class UniverseConfig extends YamlConfiguration
{
    @Comment("The main world in this universe")
    public String mainWorld;

    @Comment("Players will keep their gamemode when changing worlds in this universe")
    public boolean keepGameMode = false; // if false can use perm
    @Comment("Players will keep their flymode when changing worlds in this universe")
    public boolean keepFlyMode = false; // if false can use perm

    @Comment("If true players do not need permissions to enter this universe")
    public boolean freeAccess = true; // if false generate permission
}
