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

import java.nio.file.Path;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.ModuleManager;
import org.cubeengine.libcube.service.command.annotation.ModuleCommand;
import org.cubeengine.libcube.service.event.EventManager;
import org.cubeengine.libcube.service.i18n.I18n;
import org.cubeengine.libcube.service.inventoryguard.InventoryGuardFactory;
import org.cubeengine.libcube.service.matcher.StringMatcher;
import org.cubeengine.libcube.service.permission.PermissionManager;
import org.cubeengine.logscribe.Log;
import org.cubeengine.module.module.kits.data.KitData;
import org.cubeengine.processor.Module;
import org.cubeengine.reflect.Reflector;
import org.spongepowered.api.Server;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterCatalogEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;

@Singleton
@Module
public class Kits
{
    @Inject private KitManager kitManager;

    @Inject private EventManager em;
    @Inject private PermissionManager pm;
    private Path modulePath;
    @Inject private ModuleManager mm;
    private Log logger;

    @ModuleCommand private KitCommand kitCommand;
    @Inject private KitsPerm perms;
    @Inject private I18n i18n;

    @Listener
    public void onEnable(StartedEngineEvent<Server> event)
    {
        this.logger = mm.getLoggerFor(Kits.class);
        this.modulePath = mm.getPathFor(Kits.class);

        em.registerListener(Kits.class, kitManager);
        this.kitManager.loadKits();
    }

    @Listener
    public void onRegisterData(RegisterCatalogEvent<DataRegistration> event)
    {
        KitData.register(event);
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

    public Path getFolder()
    {
        return modulePath;
    }

    public Log getLogger()
    {
        return this.logger;
    }

    public I18n getI18n()
    {
        return this.i18n;
    }
}
