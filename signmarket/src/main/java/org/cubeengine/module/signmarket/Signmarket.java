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
import org.cubeengine.module.signmarket.data.SignTypeBuilder;
import org.cubeengine.module.signmarket.storage.TableSignBlock;
import org.cubeengine.module.signmarket.storage.TableSignItem;
import org.cubeengine.service.command.CommandManager;
import org.cubeengine.service.database.ModuleTables;
import org.cubeengine.service.event.EventManager;
import org.cubeengine.service.filesystem.ModuleConfig;
import org.spongepowered.api.Sponge;

@ModuleInfo()
@ModuleTables({TableSignItem.class, TableSignBlock.class})
public class Signmarket extends Module
{
    private MarketSignFactory marketSignFactory;
    @ModuleConfig private SignMarketConfig config;
    private EditModeListener editModeListener;
    private MarketSignPerm perms;
    private SignMarketCommands smCmds;
    @Inject private Log logger;
    @Inject private CommandManager cm;
    @Inject private EventManager em;

    public Signmarket()
    {
        Sponge.getDataManager().registerBuilder(SignType.class, new SignTypeBuilder());
        Sponge.getDataManager().register(MarketSignData.class, ImmutableMarketSignData.class, new MarketSignDataBuilder());
    }

    @Enable
    public void onEnable()
    {
        this.marketSignFactory = new MarketSignFactory(this);
        this.marketSignFactory.loadInAllSigns();
        this.editModeListener = new EditModeListener(this, i18n);
        em.registerListener(this, new MarketSignListener(this));
        smCmds = new SignMarketCommands(this, i18n);
        cm.addCommand(smCmds);
        this.perms = new MarketSignPerm(this, smCmds);

        if (!config.enableAdmin && ! config.enableUser)
        {
            logger.warn("[MarketSign] All SignTypes are disabled in the configuration!");
        }
    }

    @Override
    public void reload()
    {
        Database db = this.getCore().getDB();
        db.registerTable(TableSignItem.class); // Init Item-table first!!!
        db.registerTable(TableSignBlock.class);
        this.config = this.loadConfig(SignMarketConfig.class);
        this.marketSignFactory = new MarketSignFactory(this);
        this.marketSignFactory.loadInAllSigns();
        this.editModeListener = new EditModeListener(this, i18n);
        this.getCore().getEventManager().registerListener(this, new MarketSignListener(this));
        this.perms = new MarketSignPerm(this, smCmds);
    }

    public MarketSignFactory getMarketSignFactory()
    {
        return this.marketSignFactory;
    }

    public SignMarketConfig getConfig()
    {
        return this.config;
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
