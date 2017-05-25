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
package org.cubeengine.module.module.kits;

import java.util.List;
import java.util.stream.Collectors;
import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.completer.Completer;
import org.cubeengine.butler.parameter.reader.ArgumentReader;
import org.cubeengine.butler.parameter.reader.ReaderException;

import static org.cubeengine.libcube.util.StringUtils.startsWithIgnoreCase;

public class KitReader implements ArgumentReader<Kit>, Completer
{
    private final KitManager manager;

    public KitReader(KitManager manager)
    {
        this.manager = manager;
    }

    @Override
    public List<String> getSuggestions(CommandInvocation commandInvocation)
    {
        String token = commandInvocation.currentToken();
        return manager.getKitsNames().stream().filter(name -> startsWithIgnoreCase(name, token)).collect(Collectors.toList());
    }

    @Override
    public Kit read(Class aClass, CommandInvocation commandInvocation) throws ReaderException
    {
        String consumed = commandInvocation.consume(1);
        Kit kit = manager.getKit(consumed);
        if (kit == null)
        {
            throw new ReaderException("Kit {} not found!", consumed);
        }
        return kit;
    }
}
