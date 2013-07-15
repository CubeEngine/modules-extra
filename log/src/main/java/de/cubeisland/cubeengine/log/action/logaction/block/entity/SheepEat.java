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
package de.cubeisland.cubeengine.log.action.logaction.block.entity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.World;

import de.cubeisland.cubeengine.core.user.User;
import de.cubeisland.cubeengine.log.action.ActionTypeCategory;
import de.cubeisland.cubeengine.log.action.logaction.block.BlockActionType;
import de.cubeisland.cubeengine.log.storage.LogEntry;

import static de.cubeisland.cubeengine.log.action.ActionTypeCategory.BLOCK;
import static de.cubeisland.cubeengine.log.action.ActionTypeCategory.BLOCK_ENTITY;

/**
 * Sheeps eating grass.
 * <p>Events: {@link EntityChangeActionType}</p>
 */
public class SheepEat extends BlockActionType
{
    @Override
    protected Set<ActionTypeCategory> getCategories()
    {
        return new HashSet<ActionTypeCategory>(Arrays.asList(BLOCK, BLOCK_ENTITY));
    }

    @Override
    public String getName()
    {
        return "sheep-eat";
    }


    @Override
    protected void showLogEntry(User user, LogEntry logEntry, String time, String loc)
    {
        if (logEntry.hasAttached())
        {
            user.sendTranslated("%s&aA sheep ate all the grass%s&6 x%d",time, logEntry.getAttached().size()+1, loc);
        }
        else
        {
            user.sendTranslated("%s&aA sheep ate all the grass%s",time,loc);
        }
    }

    @Override
    public boolean isActive(World world)
    {
        return this.lm.getConfig(world).SHEEP_EAT_enable;
    }
}
