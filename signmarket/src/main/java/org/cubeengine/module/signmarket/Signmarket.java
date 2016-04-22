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
package org.cubeengine.module.signmarket;

import javax.inject.Inject;
import de.cubeisland.engine.logscribe.Log;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.cubeengine.module.signmarket.data.ImmutableMarketSignData;
import org.cubeengine.module.signmarket.data.MarketSignData;
import org.cubeengine.module.signmarket.data.MarketSignDataBuilder;
import org.cubeengine.module.signmarket.data.SignType;
import org.cubeengine.module.signmarket.data.SignTypeSerializer;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.event.EventManager;
import org.cubeengine.service.i18n.I18n;
import org.cubeengine.service.inventoryguard.InventoryGuardFactory;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.economy.EconomyService;

@ModuleInfo(name = "SignMarket", description = "Adds a sign-based market")
public class Signmarket extends Module
{
    @Inject private Log logger; // TODO log transactions
    @Inject private CommandManager cm;
    @Inject private EventManager em;
    @Inject private I18n i18n;
    @Inject private InventoryGuardFactory igf;

    private MarketSignManager manager;
    private EditModeListener editModeListener;
    private MarketSignPerm perms;
    private SignMarketCommands smCmds;

    public Signmarket()
    {
        Sponge.getDataManager().registerSerializer(SignType.class, new SignTypeSerializer());
        Sponge.getDataManager().register(MarketSignData.class, ImmutableMarketSignData.class, new MarketSignDataBuilder());
    }

    @Inject @Enable
    public void onEnable(EconomyService es)
    {
        manager = new MarketSignManager(i18n, es, this, igf);
        this.editModeListener = new EditModeListener(this, i18n, manager);
        em.registerListener(this, new MarketSignListener(manager));
        smCmds = new SignMarketCommands(this, i18n);
        cm.addCommand(smCmds);
        this.perms = new MarketSignPerm(this, smCmds);
    }

    public EditModeListener getEditModeListener()
    {
        return this.editModeListener;
    }

    public MarketSignPerm perms()
    {
        return this.perms;
    }
}
