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
package org.cubeengine.module.observe.metrics;

import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.ChangeEntityWorldEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.world.server.ServerWorld;
import org.spongepowered.observer.metrics.Meter;
import org.spongepowered.observer.metrics.meter.Gauge;

public class OnlinePlayersObserver {
    private static final Gauge PLAYERS = Meter.newGauge()
            .name("sponge_server_online_player_count")
            .help("Amount of players online")
            .build();

    private static final Gauge MAX_PLAYERS = Meter.newGauge()
            .name("sponge_server_max_player_count")
            .help("Maximum online players")
            .build();

    private static final Gauge WORLD_PLAYERS = Meter.newGauge()
            .name("sponge_world_player_count")
            .help("Players online per world")
            .labelNames("world")
            .build();

    @Listener
    public void onStartup(StartedEngineEvent<Server> event) {
        updatePlayerCount();
        MAX_PLAYERS.set(Sponge.server().maxPlayers());
    }

    @Listener
    public void onJoin(ServerSideConnectionEvent.Join e) {
        updateWorldPlayerCount(e.player().world());
        updatePlayerCount();
    }

    @Listener
    public void onDisconnect(ServerSideConnectionEvent.Disconnect e) {
        updateWorldPlayerCount(e.player().world());
        updatePlayerCount();
    }

    @Listener
    public void onWorldChange(ChangeEntityWorldEvent.Post e, @First Player p) {
        updateWorldPlayerCount(e.originalWorld());
        updateWorldPlayerCount(e.destinationWorld());
    }

    private void updatePlayerCount() {
        PLAYERS.set(Sponge.server().onlinePlayers().size());
    }

    private void updateWorldPlayerCount(ServerWorld world) {
        WORLD_PLAYERS.set(world.players().size(), world.properties().key());
    }
}
