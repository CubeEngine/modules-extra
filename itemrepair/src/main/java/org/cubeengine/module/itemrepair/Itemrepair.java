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
package org.cubeengine.module.itemrepair;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.cubeisland.engine.logscribe.Log;
import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;
import org.cubeengine.reflect.Reflector;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.database.Database;
import org.cubeengine.libcube.service.database.ModuleTables;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.itemrepair.material.BaseMaterialContainer;
import org.cubeengine.module.itemrepair.material.BaseMaterialContainerConverter;
import org.cubeengine.module.itemrepair.repair.RepairBlockManager;
import org.cubeengine.module.itemrepair.repair.storage.TableRepairBlock;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.economy.EconomyService;

import java.util.Optional;

@Singleton
@Module
@ModuleTables(TableRepairBlock.class)
public class Itemrepair extends CubeEngineModule
{
    @ModuleConfig private ItemrepairConfig config;
    public RepairBlockManager repairBlockManager;
    @Inject private Database db;
    @Inject private EventManager em;
    @Inject private CommandManager cm;
    @Inject private I18n i18n;
    private Log logger;
    @Inject private PermissionManager pm;
    @Inject private PluginContainer plugin;

    @Inject
    public Itemrepair(Reflector reflector, ModuleManager mm)
    {
        this.logger = mm.getLoggerFor(Itemrepair.class);
        reflector.getDefaultConverterManager().registerConverter(new BaseMaterialContainerConverter(), BaseMaterialContainer.class);
    }

    @Listener
    public void onEnable(GamePostInitializationEvent event)
    {
        Optional<EconomyService> economy = Sponge.getServiceManager().provide(EconomyService.class);
        if (!economy.isPresent())
        {
            this.logger.error("Missing required EconomyService. Do you have a plugin installed that provides it?");
            this.logger.info("Listeners and Commands will not be registered.");
            return;
        }
        this.repairBlockManager = new RepairBlockManager(this, db, em, i18n, economy.get(), pm);
        em.registerListener(Itemrepair.class, new ItemRepairListener(this, i18n));
        cm.addCommand(new ItemRepairCommands(cm, this, em, i18n));
    }

    public ItemrepairConfig getConfig()
    {
        return config;
    }

    public RepairBlockManager getRepairBlockManager()
    {
        return repairBlockManager;
    }

    public Log getLog()
    {
        return logger;
    }

    public PluginContainer getPlugin() {
        return plugin;
    }
}
