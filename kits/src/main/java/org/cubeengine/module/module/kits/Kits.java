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
package org.cubeengine.module.module.kits;

import java.nio.file.Path;
import javax.inject.Inject;
import de.cubeisland.engine.modularity.asm.marker.ModuleInfo;
import de.cubeisland.engine.modularity.core.Module;
import de.cubeisland.engine.modularity.core.marker.Enable;
import org.cubeengine.reflect.Reflector;
import org.cubeengine.libcube.service.command.CommandManager;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.inventoryguard.InventoryGuardFactory;
import org.cubeengine.libcube.service.matcher.StringMatcher;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.module.module.kits.data.ImmutableKitData;
import org.cubeengine.module.module.kits.data.KitData;
import org.cubeengine.module.module.kits.data.KitDataBuilder;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.plugin.PluginContainer;

@ModuleInfo(name = "Kits", description = "Hand kits to your players")
public class Kits extends Module
{
    private KitManager kitManager;

    @Inject private KitsPerm perms;

    @Inject private CommandManager cm;
    @Inject private EventManager em;
    @Inject private PermissionManager pm;
    @Inject private I18n i18n;
    @Inject private InventoryGuardFactory igf;
    @Inject private Reflector reflector;
    @Inject private Path modulePath;
    @Inject private StringMatcher sm;
    @Inject private PluginContainer plugin;

    @Inject @Enable
    public void onEnable(PluginContainer plugin)
    {
        DataRegistration<KitData, ImmutableKitData> dr = DataRegistration.<KitData, ImmutableKitData>builder()
                .dataClass(KitData.class).immutableClass(ImmutableKitData.class)
                .builder(new KitDataBuilder()).manipulatorId("kits")
                .dataName("CubeEngine Kits Data")
                .buildAndRegister(plugin);
        Sponge.getDataManager().registerLegacyManipulatorIds(KitData.class.getName(), dr);

        this.kitManager = new KitManager(this, reflector, sm);
        this.kitManager.loadKits();
        em.registerListener(Kits.class, kitManager);
        cm.getProviderManager().register(this, new KitReader(kitManager), Kit.class);
        cm.addCommand(new KitCommand(this, i18n, igf, cm));
    }

    public KitManager getKitManager()
    {
        return this.kitManager;
    }
    public KitsPerm perms()
    {
        return perms;
    }


    public PermissionManager getPermissionManager()
    {
        return pm;
    }

    public CommandManager getCommandManager()
    {
        return cm;
    }

    public Path getFolder()
    {
        return modulePath;
    }

    public PluginContainer getPlugin() {
        return plugin;
    }
}
