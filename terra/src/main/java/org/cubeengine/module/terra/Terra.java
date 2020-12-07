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
import org.cubeengine.libcube.service.event.ModuleListener;
import org.cubeengine.module.terra.data.TerraData;
import org.cubeengine.module.terra.data.TerraItems;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Server;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterCatalogEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.item.recipe.RecipeRegistration;
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
    @Inject private PluginContainer plugin;

    @Listener
    public void onStarted(StartedEngineEvent<Server> event)
    {
    }

    @Listener
    public void onRegisterRecipe(RegisterCatalogEvent<RecipeRegistration> event)
    {
        TerraItems.registerRecipes(event);
    }

    @Listener
    public void onRegisterData(RegisterCatalogEvent<DataRegistration>  event)
    {
        TerraData.register(event);
    }

}
