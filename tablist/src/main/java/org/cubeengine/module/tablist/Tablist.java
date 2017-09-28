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
package org.cubeengine.module.tablist;

import static org.spongepowered.api.text.serializer.TextSerializers.FORMATTING_CODE;

import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.processor.Dependency;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.tab.TabListEntry;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;

import java.util.Optional;

import javax.inject.Singleton;

@Singleton
@Module(dependencies = @Dependency("cubeengine-bigdata"))
public class Tablist extends CubeEngineModule
{
    @ModuleConfig private TablistConfig config;

    @Listener
    public void onEnable(GameInitializationEvent event)
    {
        
    }

    @Listener
    public void onJoin(ClientConnectionEvent.Join event)
    {
        Player player = event.getTargetEntity();
        String prefix = player.getOption("tablist-prefix").orElse("");

        if (this.config.header != null && !this.config.header.isEmpty())
        {
            player.getTabList().setHeader(FORMATTING_CODE.deserialize(this.config.header));
        }
        for (Player p : Sponge.getServer().getOnlinePlayers())
        {
            Optional<TabListEntry> entry = p.getTabList().getEntry(player.getUniqueId());
            entry.ifPresent(tle -> tle.setDisplayName(FORMATTING_CODE.deserialize(prefix + player.getName())));
        }
    }
}
