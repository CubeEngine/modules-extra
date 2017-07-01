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
package org.cubeengine.module.signmarket;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.cubeisland.engine.logscribe.Log;
import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.InjectService;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.signmarket.data.ImmutableMarketSignData;
import org.cubeengine.module.signmarket.data.MarketSignData;
import org.cubeengine.module.signmarket.data.MarketSignDataBuilder;
import org.cubeengine.module.signmarket.data.SignType;
import org.cubeengine.module.signmarket.data.SignTypeSerializer;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.inventoryguard.InventoryGuardFactory;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.economy.EconomyService;

@Singleton
@Module
public class Signmarket extends CubeEngineModule
{
    private Log logger; // TODO log transactions
    @Inject private CommandManager cm;
    @Inject private EventManager em;
    @Inject private I18n i18n;
    @Inject private InventoryGuardFactory igf;
    @Inject private PermissionManager pm;
    @Inject private PluginContainer plugin;
    @Inject private ModuleManager mm;
    @InjectService private EconomyService es;

    private MarketSignManager manager;
    private EditModeCommand editModeListener;
    private MarketSignPerm perms;
    private SignMarketCommands smCmds;

    @Inject
    public Signmarket(PluginContainer plugin)
    {
        Sponge.getDataManager().registerTranslator(SignType.class, new SignTypeSerializer());
        DataRegistration<MarketSignData, ImmutableMarketSignData> dr =
                DataRegistration.<MarketSignData, ImmutableMarketSignData>builder()
                        .dataClass(MarketSignData.class).immutableClass(ImmutableMarketSignData.class)
                        .builder(new MarketSignDataBuilder()).manipulatorId("market_sign")
                        .dataName("CubeEngine SignMarket Data")
                        .buildAndRegister(plugin);

        Sponge.getDataManager().registerLegacyManipulatorIds(MarketSignData.class.getName(), dr);

    }

    @Listener
    public void onEnable(GamePostInitializationEvent event)
    {
        this.logger = mm.getLoggerFor(Signmarket.class);
        manager = new MarketSignManager(i18n, es, this, igf, plugin);
        editModeListener = new EditModeCommand(cm, em, this, i18n, manager);
        em.registerListener(Signmarket.class, new MarketSignListener(manager, this, i18n));
        smCmds = new SignMarketCommands(cm, this, i18n);
        cm.addCommand(smCmds);
        perms = new MarketSignPerm(pm, smCmds);
    }

    public EditModeCommand getEditModeCommand()
    {
        return this.editModeListener;
    }

    public MarketSignPerm perms()
    {
        return this.perms;
    }
}
