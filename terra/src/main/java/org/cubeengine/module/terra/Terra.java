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
package org.cubeengine.module.terra;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.cubeengine.libcube.service.command.annotation.ModuleCommand;
import org.cubeengine.libcube.service.event.ModuleListener;
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.module.terra.data.TerraData;
import org.cubeengine.module.terra.data.TerraItems;
import org.cubeengine.processor.Module;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterDataEvent;
import org.spongepowered.api.event.lifecycle.RegisterDataPackValueEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.item.recipe.RecipeRegistration;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.world.WorldTypeTemplate;
import org.spongepowered.plugin.PluginContainer;

/*
command to give terra-essence potion
splash to teleport multiple players
*/

@Singleton
@Module
public class Terra
{
    @ModuleListener private TerraListener listener;
    @ModuleCommand private TerraCommands terraCommands;
    @Inject private PluginContainer plugin;
    @Inject private TaskManager tm;

    public static final ResourceKey WORLD_TYPE_END = ResourceKey.of(PluginTerra.TERRA_ID, "the_end");

    @Listener
    public void onStarted(StartedEngineEvent<Server> event)
    {
        final Ticks minutes = Ticks.ofWallClockMinutes(Sponge.server(), 10);
        tm.runTimer(this.listener::checkForUnload, Ticks.ofWallClockSeconds(Sponge.server(), 10), minutes);
        tm.runTimer(task -> this.listener.doGenerate(), Ticks.of(20), Ticks.of(20));
    }

    @Listener
    public void onRegisterRecipe(RegisterDataPackValueEvent<RecipeRegistration>event)
    {
        TerraItems.registerRecipes(event, this);
    }

    @Listener
    public void onRegisterWorldType(RegisterDataPackValueEvent<WorldTypeTemplate> event)
    {
        event.register(WorldTypeTemplate.builder().from(WorldTypeTemplate.theEnd()).key(WORLD_TYPE_END).createDragonFight(false).build());
    }

    @Listener
    public void onRegisterData(RegisterDataEvent event)
    {
        TerraData.register(event);
    }

    public TerraListener getListener()
    {
        return this.listener;
    }
}
