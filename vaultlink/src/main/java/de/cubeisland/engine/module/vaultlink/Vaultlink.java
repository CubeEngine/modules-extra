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
import de.cubeisland.engine.module.roles.Roles;
import de.cubeisland.engine.module.vaultlink.service.CubeChatService;
import de.cubeisland.engine.module.vaultlink.service.CubeEconomyService;
import de.cubeisland.engine.module.vaultlink.service.CubePermissionService;
import de.cubeisland.engine.module.vaultlink.service.VaultEconomyService;
import de.cubeisland.engine.module.vaultlink.service.VaultMetadataService;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

// TODO declare service providing. possibly at runtime?
public class Vaultlink extends Module implements Listener
{
    private final AtomicReference<de.cubeisland.engine.core.module.service.Economy> economyReference = new AtomicReference<>();
    private final AtomicReference<Metadata> metadataReference = new AtomicReference<>();
    private final AtomicReference<Chat> vaultChatReference = new AtomicReference<>();
    private final AtomicReference<Economy> vaultEconomyReference = new AtomicReference<>();
    private BukkitServiceManager bukkitServiceManager;
    private Permission vaultPermService;
    private Chat vaultChatService;
    private Economy vaultEconService;

    @Override
    public void onLoad()
    {
        this.bukkitServiceManager = ((BukkitCore)this.getCore()).getModuleManager().getServiceManager();
        Module module = getCore().getModuleManager().getModule("roles");
        if (module != null && module instanceof Roles)
        {
            Roles roles = (Roles)module;
            vaultPermService = new CubePermissionService(this, roles);
            vaultChatService = new CubeChatService(this, metadataReference, vaultPermService);

            this.bukkitServiceManager.register(Permission.class, vaultPermService, this, ServicePriority.Low);
            this.bukkitServiceManager.register(Chat.class, vaultChatService, this, ServicePriority.Low);
        }
        vaultEconService = new CubeEconomyService(this, economyReference);
        this.bukkitServiceManager.register(Economy.class, vaultEconService, this, ServicePriority.Low);
    }

    @Override
    public void onEnable()
    {
        Core core = getCore();
        ServiceManager serviceManager = core.getModuleManager().getServiceManager();

        this.getCore().getEventManager().registerListener(this, this);
        if (serviceManager.isImplemented(de.cubeisland.engine.core.module.service.Economy.class))
        {
            this.economyReference.set(serviceManager.getServiceImplementation(
                de.cubeisland.engine.core.module.service.Economy.class));
        }
        else
        {
            this.bukkitServiceManager.unregister(Economy.class, vaultEconService);
            this.vaultEconService = null;
            if (this.bukkitServiceManager.isProvidedFor(Economy.class))
            {
                vaultEconService = this.bukkitServiceManager.load(Economy.class);
                vaultEconomyReference.set(vaultEconService);
                serviceManager.registerService(this, de.cubeisland.engine.core.module.service.Economy.class,
                                               new VaultEconomyService(this.vaultEconomyReference));
            }
        }
        if (serviceManager.isImplemented(Metadata.class))
        {
            this.metadataReference.set(serviceManager.getServiceImplementation(Metadata.class));
        }
        else
        {
            this.bukkitServiceManager.unregister(Chat.class, vaultChatService);
            this.vaultChatService = null;
            if (this.bukkitServiceManager.isProvidedFor(Chat.class))
            {
                vaultChatService = this.bukkitServiceManager.load(Chat.class);
                vaultChatReference.set(vaultChatService);
                serviceManager.registerService(this, Metadata.class, new VaultMetadataService(this.vaultChatReference));
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
            if (vaultEconomyReference.get() != load)
            {
                vaultEconomyReference.set(load);
            }
        }
        if (Chat.class.equals(event.getProvider().getService()))
        {
            Chat load = bukkitServiceManager.load(Chat.class);
            if (vaultChatReference.get() != load)
            {
                vaultChatReference.set(load);
            }
        }
    }
}
