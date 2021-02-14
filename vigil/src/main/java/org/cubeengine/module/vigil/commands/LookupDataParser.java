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
package org.cubeengine.module.vigil.commands;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.DefaultParameterProvider;
import org.cubeengine.libcube.service.command.annotation.ParserFor;
import org.cubeengine.module.vigil.data.LookupData;
import org.cubeengine.module.vigil.report.block.BlockReport;
import org.cubeengine.module.vigil.report.block.ExplosionReport;
import org.cubeengine.module.vigil.report.entity.DestructReport;
import org.cubeengine.module.vigil.report.inventory.ChangeInventoryReport;
import org.cubeengine.module.vigil.report.inventory.InventoryOpenReport;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.exception.ArgumentParseException;
import org.spongepowered.api.command.parameter.ArgumentReader.Mutable;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.CommandContext.Builder;
import org.spongepowered.api.command.parameter.Parameter.Key;
import org.spongepowered.api.command.parameter.managed.ValueCompleter;
import org.spongepowered.api.command.parameter.managed.ValueParser;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

@Singleton
@ParserFor(LookupData.class)
public class LookupDataParser implements ValueParser<LookupData>, ValueCompleter, DefaultParameterProvider<LookupData>
{
    private Map<String, LookupData> types = new HashMap<>();
    private LookupData defaultType;

    @Inject
    public LookupDataParser()
    {
        this.defaultType = new LookupData();
        this.types.put("chest", this.defaultType.copy().setReports(ChangeInventoryReport.class, InventoryOpenReport.class));
        // TODO this.types.put("player", this.defaultType.copy());
        this.types.put("kills", this.defaultType.copy().setReports(DestructReport.class));
        this.types.put("block", this.defaultType.copy().setReports(BlockReport.class, ExplosionReport.class));
    }

    @Override
    public LookupData apply(CommandCause cause)
    {
        return defaultType.copy().setCreator(((ServerPlayer) cause.getSubject()).getUniqueId());
    }

    @Override
    public List<String> complete(CommandContext context, String currentInput)
    {
        return this.types.keySet().stream().filter(k -> k.startsWith(currentInput)).collect(Collectors.toList());
    }

    @Override
    public Optional<? extends LookupData> getValue(Key<? super LookupData> parameterKey, Mutable reader, Builder context) throws ArgumentParseException
    {
        String token = reader.parseString();
        if (!types.containsKey(token.toLowerCase()))
        {
            return Optional.empty();
            // TODO error msg throw new TranslatedParserException(i18n.translate(ci.getContext(Locale.class), NEGATIVE, "{input} is not a valid log-type. Use chest, container, player, block or kills instead!", token));
        }
        LookupData data = types.get(token.toLowerCase()).copy();
        return Optional.of(data.setCreator(((ServerPlayer) context.getSubject()).getUniqueId()));
    }

}
