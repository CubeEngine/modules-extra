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
package org.cubeengine.module.itemduct;

import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.service.event.ModuleListener;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.module.itemduct.listener.ItemDuctFilterListener;
import org.cubeengine.module.itemduct.listener.ItemDuctListener;
import org.cubeengine.module.itemduct.listener.ItemDuctTransferListener;
import org.cubeengine.processor.Module;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.plugin.PluginContainer;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Module
public class Itemduct extends CubeEngineModule
{
    @ModuleConfig private ItemductConfig config;
    @Inject private ItemDuctManager manager;
    @ModuleListener private ItemDuctListener listenerActivator;
    @ModuleListener private ItemDuctTransferListener listenerTransfer;
    @ModuleListener private ItemDuctFilterListener listenerFilter;
    @Inject private PluginContainer plugin;

    @Listener
    public void onPreInit(GamePreInitializationEvent event)
    {
        this.manager.setup(this.plugin, this.config);
        this.listenerActivator.setup();
        this.listenerFilter.setup();
        this.listenerTransfer.setup(this.manager);
    }

    @Listener
    public void onReload(GameReloadEvent event)
    {
        this.config.reload();
        this.manager.reload(this.config);
    }
}
