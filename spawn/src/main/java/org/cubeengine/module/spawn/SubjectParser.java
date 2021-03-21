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
package org.cubeengine.module.spawn;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.DefaultParameterProvider;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.parameter.ArgumentReader.Mutable;
import org.spongepowered.api.command.parameter.CommandContext.Builder;
import org.spongepowered.api.command.parameter.Parameter.Key;
import org.spongepowered.api.command.parameter.managed.ValueParser;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;

@Singleton
public class SubjectParser implements ValueParser<Subject>, DefaultParameterProvider<Subject>
{
    @Override
    public Subject apply(CommandCause commandCause)
    {
        try
        {
            if (commandCause.audience() instanceof ServerPlayer)
            {
                final PermissionService pm = Sponge.server().serviceProvider().permissionService();
                return pm.userSubjects().loadSubject((((ServerPlayer)commandCause.audience())).identifier()).get();
            }
        }
        catch (ExecutionException | InterruptedException e)
        {
            throw new IllegalStateException(e); // TODO better handling
        }
        // TODO exception
        return null;
    }

    @Override
    public Optional<? extends Subject> parseValue(Key<? super Subject> parameterKey, Mutable reader, Builder context) throws ArgumentParseException
    {
        try
        {
            final PermissionService pm = Sponge.server().serviceProvider().permissionService();

            String token = reader.parseString();
            if (pm.groupSubjects().hasSubject(token).get())
            {
                return Optional.of(pm.groupSubjects().loadSubject(token).join());
            }
        }
        catch (ExecutionException | InterruptedException e)
        {
            throw new IllegalStateException(e); // TODO better handling
        }

        // TODO msg i18n.sendTranslated(ctx, NEGATIVE, "Could not find the role {input}!", role, context);
        return null;
    }

}
