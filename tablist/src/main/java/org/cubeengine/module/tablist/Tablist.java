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

import com.google.inject.Singleton;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.cubeengine.libcube.util.ChatFormat;
import org.cubeengine.processor.Module;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.entity.living.player.tab.TabListEntry;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;

@Singleton
@Module
public class Tablist
{
    @ModuleConfig private TablistConfig config;

    @Listener
    public void onJoin(ServerSideConnectionEvent.Join event)
    {
        ServerPlayer player = event.player();
        String prefix = player.option("tablist-prefix").orElse("");

        if (this.config.header != null && !this.config.header.isEmpty())
        {
            player.tabList().setHeader(ChatFormat.fromLegacy(this.config.header, '&'));
        }

        for (ServerPlayer p : Sponge.server().onlinePlayers())
        {
            final TabListEntry entry = p.tabList().entry(player.uniqueId()).orElse(
                TabListEntry.builder().list(p.tabList()).profile(player.profile()).displayName(player.displayName().get()).gameMode(player.gameMode().get()).build());
            entry.setDisplayName(ChatFormat.fromLegacy(prefix + player.name(), '&'));

            String pPrefix = p.option("tablist-prefix").orElse("");
            TabListEntry pEntry = player.tabList().entry(p.uniqueId()).orElse(
                TabListEntry.builder().list(player.tabList()).profile(p.profile()).displayName(p.displayName().get()).gameMode(p.gameMode().get()).build());
            pEntry.setDisplayName(ChatFormat.fromLegacy(pPrefix + p.name(), '&'));
        }
    }
}
