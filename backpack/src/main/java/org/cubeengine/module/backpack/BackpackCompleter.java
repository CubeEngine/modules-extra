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
package org.cubeengine.module.backpack;

import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.completer.Completer;
import org.spongepowered.api.entity.living.player.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BackpackCompleter implements Completer
{

    private BackpackManager manager;

    public BackpackCompleter(BackpackManager manager)
    {

        this.manager = manager;
    }

    @Override
    public List<String> getSuggestions(CommandInvocation invocation)
    {
        if (invocation.getCommandSource() instanceof Player)
        {
            List<String> list = new ArrayList<>();
            String token = invocation.currentToken();
            for (String name : manager.getBackpackNames(((Player) invocation.getCommandSource()).getUniqueId()))
            {
                if (name.startsWith(token))
                {
                    list.add(name);
                }
            }
            return list;
        }
        return Collections.emptyList();
    }
}
