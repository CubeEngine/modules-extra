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
package de.cubeisland.engine.module.log.action;

import java.util.ArrayList;
import java.util.List;

import de.cubeisland.engine.command.completer.Completer;
import de.cubeisland.engine.command.methodic.context.BaseCommandContext;

public class ActionTypeCompleter implements Completer
{
    private ActionManager manager;

    public ActionTypeCompleter(ActionManager manager)
    {
        this.manager = manager;
    }

    @Override
    public List<String> complete(BaseCommandContext context, String token)
    {
        List<String> result = new ArrayList<>();
        String lastToken = token.toLowerCase();
        String firstTokens = "";
        if (lastToken.contains(","))
        {
            firstTokens = lastToken.substring(0, lastToken.lastIndexOf(",")+1);
            lastToken = lastToken.substring(lastToken.lastIndexOf(",")+1,lastToken.length());
        }
        if (lastToken.startsWith("!"))
        {
            lastToken = lastToken.substring(1, lastToken.length());
            firstTokens += "!";
        }
        for (String s : manager.getAllActionAndCategoryStrings())
        {
            String compare = s.toLowerCase();
            if (compare.startsWith(lastToken))
            {
                if (!token.contains(compare+","))
                {
                    result.add(firstTokens + s);
                }
            }
        }
        return result;
    }
}
