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
package de.cubeisland.engine.module.vaultlink;

import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceEvent;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;

import de.cubeisland.engine.core.Core;
import de.cubeisland.engine.core.bukkit.BukkitCore;
import de.cubeisland.engine.core.bukkit.BukkitServiceManager;
import de.cubeisland.engine.core.module.Module;
import de.cubeisland.engine.core.module.service.Metadata;
import de.cubeisland.engine.core.module.service.ServiceManager;
import de.cubeisland.engine.module.vaultlink.service.CubeChatService;
import de.cubeisland.engine.module.vaultlink.service.CubeEconomyService;
import de.cubeisland.engine.module.vaultlink.service.CubePermissionService;
import de.cubeisland.engine.module.vaultlink.service.VaultEconomyService;
import de.cubeisland.engine.module.vaultlink.service.VaultMetadataService;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

public class Vaultlink extends Module implements Listener
{
    private final AtomicReference<de.cubeisland.engine.core.module.service.Permission> permRef = new AtomicReference<>();
    private final AtomicReference<Metadata> metaRef = new AtomicReference<>();
    private final AtomicReference<de.cubeisland.engine.core.module.service.Economy> econRef = new AtomicReference<>();
    private final AtomicReference<Permission> vaultPermRef = new AtomicReference<>();
    private final AtomicReference<Chat> vaultChatRef = new AtomicReference<>();
    private final AtomicReference<Economy> vaultEconRef = new AtomicReference<>();
    private BukkitServiceManager bukkitServiceManager;

    @Override
    public void onLoad()
    {
        this.bukkitServiceManager = ((BukkitCore)this.getCore()).getModuleManager().getServiceManager();

        // Permission & Metadata/Chat
        vaultPermRef.set(new CubePermissionService(this, permRef));
        vaultChatRef.set(new CubeChatService(this, metaRef, vaultPermRef.get()));
        this.bukkitServiceManager.register(Permission.class, vaultPermRef.get(), this, ServicePriority.Low);
        this.bukkitServiceManager.register(Chat.class, vaultChatRef.get(), this, ServicePriority.Low);

        // Economy
        vaultEconRef.set(new CubeEconomyService(this, econRef));
        this.bukkitServiceManager.register(Economy.class, vaultEconRef.get(), this, ServicePriority.Low);
    }

    @Override
    public void onEnable()
    {
        Core core = getCore();
        ServiceManager serviceManager = core.getModuleManager().getServiceManager();

        this.getCore().getEventManager().registerListener(this, this);
        if (serviceManager.isImplemented(de.cubeisland.engine.core.module.service.Economy.class))
        {
            this.econRef.set(serviceManager.getServiceImplementation(
                de.cubeisland.engine.core.module.service.Economy.class));
        }
        else
        {
            this.bukkitServiceManager.unregister(Economy.class, vaultEconRef.get());
            this.vaultEconRef.set(null);
            if (this.bukkitServiceManager.isProvidedFor(Economy.class))
            {
                vaultEconRef.set(this.bukkitServiceManager.load(Economy.class));
                serviceManager.registerService(this, de.cubeisland.engine.core.module.service.Economy.class,
                                               new VaultEconomyService(this.vaultEconRef));
            }
        }
        if (serviceManager.isImplemented(de.cubeisland.engine.core.module.service.Permission.class))
        {
            this.permRef.set(serviceManager.getServiceImplementation(
                de.cubeisland.engine.core.module.service.Permission.class));
        }
        else
        {
            this.bukkitServiceManager.unregister(vaultPermRef.get());
            this.vaultPermRef.set(null);
            if (this.bukkitServiceManager.isProvidedFor(Permission.class))
            {
                vaultPermRef.set(this.bukkitServiceManager.load(Permission.class));
                serviceManager.registerService(this, de.cubeisland.engine.core.module.service.Permission.class, new VaultPermissionService(this.vaultPermRef));
            }
        }
        if (serviceManager.isImplemented(Metadata.class))
        {
            this.metaRef.set(serviceManager.getServiceImplementation(Metadata.class));
        }
        else
        {
            this.bukkitServiceManager.unregister(Chat.class, vaultChatRef.get());
            this.vaultChatRef.set(null);
            if (this.bukkitServiceManager.isProvidedFor(Chat.class))
            {
                vaultChatRef.set(this.bukkitServiceManager.load(Chat.class));
                serviceManager.registerService(this, Metadata.class, new VaultMetadataService(this.vaultChatRef));
            }
        }
        core.getEventManager().registerListener(this, this);
    }

    @Override
    public void onStartupFinished()
    {
        final ServicesManager sm = Bukkit.getServicesManager();
        for (Class<?> serviceClass : sm.getKnownServices())
        {
            getLog().debug("Service: {}", serviceClass.getName());
            for (RegisteredServiceProvider<?> p : sm.getRegistrations(serviceClass))
            {
                getLog().debug(" - Provider {} ({}) [{}]", p.getProvider().getClass().getName(),
                               p.getPlugin().getName(), p.getPriority().name());
            }
        }
    }

    @EventHandler
    private void serviceRegistered(ServiceRegisterEvent event)
    {
        updateService(event);
    }

    @EventHandler
    private void serviceUnregister(ServiceUnregisterEvent event)
    {
        updateService(event);
    }

    private void updateService(ServiceEvent event)
    {
        if (Economy.class.equals(event.getProvider().getService()))
        {
            Economy load = bukkitServiceManager.load(Economy.class);
            if (vaultEconRef.get() != load)
            {
                vaultEconRef.set(load);
            }
        }
        if (Permission.class.equals(event.getProvider().getService()))
        {
            Permission load = bukkitServiceManager.load(Permission.class);
            if (vaultPermRef.get() != load)
            {
                vaultPermRef.set(load);
            }
        }
        if (Chat.class.equals(event.getProvider().getService()))
        {
            Chat load = bukkitServiceManager.load(Chat.class);
            if (vaultChatRef.get() != load)
            {
                vaultChatRef.set(load);
            }
        }
    }
}
