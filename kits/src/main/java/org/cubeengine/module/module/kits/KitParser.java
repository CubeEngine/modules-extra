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
import java.util.Optional;
import java.util.stream.Collectors;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.annotation.ParserFor;
import org.cubeengine.libcube.service.i18n.I18n;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.parameter.ArgumentReader.Mutable;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.CommandContext.Builder;
import org.spongepowered.api.command.parameter.Parameter.Key;
import org.spongepowered.api.command.parameter.managed.ValueCompleter;
import org.spongepowered.api.command.parameter.managed.ValueParser;

import static org.cubeengine.libcube.util.StringUtils.startsWithIgnoreCase;

@Singleton
@ParserFor(Kit.class)
public class KitParser implements ValueParser<Kit>, ValueCompleter
{
    private final KitManager manager;
    private I18n i18n;

    @Inject
    public KitParser(KitManager manager, I18n i18n)
    {
        this.manager = manager;
        this.i18n = i18n;
    }

    @Override
    public List<String> complete(CommandContext context, String currentInput)
    {
        return manager.getKitsNames().stream().filter(name -> startsWithIgnoreCase(name, currentInput)).collect(Collectors.toList());
    }

    @Override
    public Optional<? extends Kit> getValue(Key<? super Kit> parameterKey, Mutable reader, Builder context) throws ArgumentParseException
    {
        final String consumed = reader.parseString();
        Kit kit = manager.getKit(consumed);

        if (kit == null)
        {
            throw reader.createException(i18n.translate(context.getCause().getAudience(), "Kit {} not found!", consumed));
        }
        return Optional.of(kit);
    }
}
