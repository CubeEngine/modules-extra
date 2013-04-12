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
package de.cubeisland.cubeengine.log.action.logaction.spawn;

import java.util.EnumSet;

import org.bukkit.World;

import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.log.action.logaction.SimpleLogActionType;
import de.cubeisland.cubeengine.log.storage.LogEntry;

import static de.cubeisland.cubeengine.log.action.ActionType.Category.ENTITY;

/**
 * other spawning (by player)
 * <p>Events: {@link EntitySpawnActionType}</p>
 */
public class OtherSpawn extends SimpleLogActionType

{
    @Override
    protected EnumSet<Category> getCategories()
    {
        return EnumSet.of(ENTITY);
    }

    @Override
    public boolean canRollback()
    {
        return false;
    }
    @Override
    public String getName()
    {
        return "other-spawn";
    }

    @Override
    protected void showLogEntry(User user, LogEntry logEntry, String time, String loc)
    {//TODO get player in data once possible
        user.sendTranslated("%s&6%s &aspawned%s&a!",
                           time, logEntry.getCauserEntity(),loc);
    }

    @Override
    public boolean isSimilar(LogEntry logEntry, LogEntry other)
    {
        return logEntry.causer == other.causer
            && logEntry.world == other.world
            && logEntry.location.equals(other.location);
    }


    @Override
    public boolean isActive(World world)
    {
        return this.lm.getConfig(world).OTHER_SPAWN_enable;
    }
}