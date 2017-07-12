package org.cubeengine.module.vigil.commands;

import static org.cubeengine.libcube.service.i18n.formatter.MessageType.NEGATIVE;

import org.cubeengine.butler.CommandInvocation;
import org.cubeengine.butler.parameter.argument.ArgumentParser;
import org.cubeengine.butler.parameter.argument.Completer;
import org.cubeengine.butler.parameter.argument.DefaultValue;
import org.cubeengine.butler.parameter.argument.ParserException;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

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
        this.types.put("block", this.defaultType.copy().withReports(BreakBlockReport.class, PlaceBlockReport.class, ModifyBlockReport.class,
                ExplosionReport.class));
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
