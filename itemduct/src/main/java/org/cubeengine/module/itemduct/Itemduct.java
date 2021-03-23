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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.event.ModuleListener;
import org.cubeengine.libcube.service.filesystem.ConfigLoader;
import org.cubeengine.libcube.service.filesystem.FileManager;
import org.cubeengine.module.itemduct.data.ItemductAdvancements;
import org.cubeengine.module.itemduct.data.ItemductData;
import org.cubeengine.module.itemduct.data.ItemductItems;
import org.cubeengine.module.itemduct.listener.ItemductListener;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Server;
import org.spongepowered.api.advancement.Advancement;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.CraftItemEvent;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.event.lifecycle.RegisterDataPackValueEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.item.recipe.RecipeRegistration;
import org.spongepowered.plugin.PluginContainer;

/*
TODO API8 Port:
Reload event

Future Ideas:
Introduce more ways to filter items
Upgrades?
 */
@Singleton
@Module
public class Itemduct
{
    private ItemductConfig config;
    @Inject private ItemductManager manager;
    @ModuleListener private ItemductListener listenerActivator;
    @Inject private PluginContainer plugin;
    @Inject private ConfigLoader cl;


    @Listener
    public void onStarted(StartedEngineEvent<Server> event)
    {
        this.manager.setup(this.config);
    }

//    @Listener
//    public void onReload(Reload event)
//    {
//        this.config.reload();
//        this.manager.reload(this.config);
//    }

    @Listener
    public void onRegisterDatapacks(RegisterDataPackValueEvent<RecipeRegistration>event)
    {
        this.config = cl.loadConfig(ItemductConfig.class);
        ItemductItems.registerRecipes(event, config); // TODO config is not loaded yet :(
    }

    @Listener
    public void onRegisterAdvancements(RegisterDataPackValueEvent<Advancement> event)
    {
        ItemductAdvancements.init(plugin);
        ItemductAdvancements.register(event);
    }

    @Listener
    public void onRegisterData(RegisterDataEvent event)
    {
        ItemductData.register(event);
    }

    @Listener
    public void onCraft(CraftItemEvent.Craft event, @Root ServerPlayer player)
    {
        if (event.recipe().isPresent() && ItemductItems.matchesRecipe(event.recipe().get())) {
            player.progress(ItemductAdvancements.ROOT).grant();
        }
    }

}
