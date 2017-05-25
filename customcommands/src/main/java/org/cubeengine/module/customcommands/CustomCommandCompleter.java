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
package org.cubeengine.module.customcommands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.completer.Completer;

import static java.util.Locale.ENGLISH;

public class CustomCommandCompleter implements Completer
{
    private Customcommands module;

    public CustomCommandCompleter(Customcommands module)
    {
        this.module = module;
    }

    @Override
    public List<String> suggest(Class type, CommandInvocation invocation)
    {
        ArrayList<String> list = new ArrayList<>();
        for (String item : module.getConfig().commands.keySet())
        {
            if (item.startsWith(invocation.currentToken().toLowerCase(ENGLISH)))
            {
                list.add(item);
            }
        }
        Collections.sort(list);
        return list;
    }
}
