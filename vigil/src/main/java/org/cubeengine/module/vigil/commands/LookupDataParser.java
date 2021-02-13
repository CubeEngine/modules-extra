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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.cubeengine.libcube.service.command.TranslatedParserException;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.module.vigil.data.LookupData;
import org.cubeengine.module.vigil.report.block.BreakBlockReport;
import org.cubeengine.module.vigil.report.block.ExplosionReport;
import org.cubeengine.module.vigil.report.block.ModifyBlockReport;
import org.cubeengine.module.vigil.report.block.PlaceBlockReport;
import org.cubeengine.module.vigil.report.entity.DestructReport;
import org.cubeengine.module.vigil.report.inventory.ChangeInventoryReport;
import org.cubeengine.module.vigil.report.inventory.InventoryOpenReport;
import org.spongepowered.api.entity.living.player.Player;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;

public class LookupDataParser implements ArgumentParser<LookupData>, Completer, DefaultValue<LookupData>
{
    private Map<String, LookupData> types = new HashMap<>();
    private LookupData defaultType;

    private I18n i18n;

    public LookupDataParser(I18n i18n)
    {
        this.i18n = i18n;
        this.defaultType = new LookupData();
        this.types.put("chest", this.defaultType.copy().withReports(ChangeInventoryReport.class, InventoryOpenReport.class));
        // TODO this.types.put("player", this.defaultType.copy());
        this.types.put("kills", this.defaultType.copy().withReports(DestructReport.class));
        this.types.put("block", this.defaultType.copy().withReports(BreakBlockReport.class, PlaceBlockReport.class, ModifyBlockReport.class, ExplosionReport.class));
    }

    @Override
    public LookupData parse(Class clazz, CommandInvocation ci) throws ParserException
    {
        String token = ci.consume(1);
        if (!types.keySet().contains(token.toLowerCase()))
        {
            throw new TranslatedParserException(i18n.translate(ci.getContext(Locale.class), NEGATIVE, "{input} is not a valid log-type. Use chest, container, player, block or kills instead!", token));
        }
        LookupData data = types.get(token.toLowerCase()).copy();
        return data.withCreator(((Player) ci.getCommandSource()).getUniqueId());
    }

    @Override
    public List<String> suggest(Class clazz, CommandInvocation ci)
    {
        String token = ci.currentToken();
        return new ArrayList<>(this.types.keySet().stream().filter(k -> k.startsWith(token)).collect(Collectors.toList()));
    }

    @Override
    public LookupData provide(CommandInvocation ci)
    {
        return defaultType.copy().withCreator(((Player) ci.getCommandSource()).getUniqueId());
    }
}
