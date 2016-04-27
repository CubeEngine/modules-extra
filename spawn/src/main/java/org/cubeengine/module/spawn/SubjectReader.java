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
package org.cubeengine.module.spawn;

import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.parameter.reader.ArgumentReader;
import org.cubeengine.butler.parameter.reader.DefaultValue;
import org.cubeengine.butler.parameter.reader.ReaderException;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.permission.option.OptionSubject;

import static org.cubeengine.service.i18n.formatter.MessageType.NEGATIVE;

public class SubjectReader implements ArgumentReader<OptionSubject>, DefaultValue<OptionSubject>
{
    private PermissionService pm;

    public SubjectReader(PermissionService pm)
    {
        this.pm = pm;
    }

    @Override
    public OptionSubject read(Class aClass, CommandInvocation commandInvocation) throws ReaderException
    {
        String token = commandInvocation.currentToken();
        if (pm.getGroupSubjects().hasRegistered(token))
        {
            return ((OptionSubject)pm.getGroupSubjects().get(token));
        }

        // TODO msg i18n.sendTranslated(ctx, NEGATIVE, "Could not find the role {input}!", role, context);
        return null;
    }

    @Override
    public OptionSubject getDefault(CommandInvocation invocation)
    {
        if (invocation.getCommandSource() instanceof Player)
        {
            return ((OptionSubject)pm.getUserSubjects().get(((Player)invocation.getCommandSource()).getIdentifier()));
        }
        // TODO exception
        return null;
    }
}
