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
package org.cubeengine.module.stats;

import com.flowpowered.math.vector.Vector3i;
import org.cubeengine.processor.Module;
import org.cubeengine.libcube.CubeEngineModule;
import org.cubeengine.libcube.service.filesystem.ModuleConfig;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.animal.Animal;
import org.spongepowered.api.entity.living.monster.Monster;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import javax.inject.Singleton;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.influxdb.InfluxDB.ConsistencyLevel.ALL;

@Singleton
@Module
public class Stats extends CubeEngineModule
{
    @ModuleConfig private StatsConfig config;

    private InfluxDB connection;

    @Listener
    public void onPreInit(GamePreInitializationEvent event)
    {
        connection = InfluxDBFactory.connect(config.url, config.user, config.password);
        connection.createDatabase(config.database);
        connection.setDatabase(config.database);
    }

    private BatchPoints newBatch() {
        return BatchPoints
                .database(config.database)
                .tag("async", "true")
                .consistency(ALL)
                .build();
    }

    private Point.Builder newPoint(String name) {
        return Point.measurement("block_break")
                .time(Instant.now().toEpochMilli(), TimeUnit.MILLISECONDS);
    }

    public void onBlockBreak(ChangeBlockEvent.Break.Post event, @Root Player player) {
        handleChangeBlock("block_break", event, player);
    }

    public void onBlockPlace(ChangeBlockEvent.Break.Post event, @Root Player player) {
        handleChangeBlock("block_place", event, player);
    }

    public void onBlockModify(ChangeBlockEvent.Modify.Post event, @Root Player player) {
        handleChangeBlock("block_place", event, player);
    }

    private void handleChangeBlock(String name, ChangeBlockEvent.Break.Post event, Player player) {
        BatchPoints batch = newBatch();
        for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {
            Point.Builder p = newPoint(name);
            addPlayer(p, player);
            BlockSnapshot original = transaction.getOriginal();
            p.tag("block_original", original.getState().getId());
            p.tag("block_final", transaction.getFinal().getState().getId());
            original.getLocation().ifPresent(loc -> {
                addLocation(p, loc);
            });

            batch.point(p.build());
        }

        connection.write(batch);
    }

    private void addPlayer(Point.Builder p, Player player) {
        p.tag("player_id", player.getUniqueId().toString());
        p.addField("player_name", player.getName());
    }

    private void addLocation(Point.Builder p, Location<World> loc) {
        p.addField("x", loc.getX());
        p.addField("y", loc.getY());
        p.addField("z", loc.getY());
        Vector3i chunkPos = loc.getChunkPosition();
        p.addField("chunk_x", chunkPos.getX());
        p.addField("chunk_y", chunkPos.getY());
        p.addField("chunk_z", chunkPos.getZ());
        p.tag("world_id", loc.getExtent().getUniqueId().toString());
        p.addField("world_name", loc.getExtent().getName());
    }

    public void onPlayerDeath(DestructEntityEvent.Death event, @Getter("getTargetEntity") Player player) {
        Point.Builder p = newPoint("player_death");
        addPlayer(p, player);
        addLocation(p, player.getLocation());
        connection.write(p.build());
    }

    public void onAnimalDeath(DestructEntityEvent.Death event, @Root Player player, @Getter("getTargetEntity") Animal animal) {
        Point.Builder p = newPoint("animal_death");
        addPlayer(p, player);
        EntityType type = animal.getType();
        p.tag("animal_type", type.getId());
        p.addField("animal_name", type.getName());
        addLocation(p, animal.getLocation());
        connection.write(p.build());
    }

    public void onMonsterDeath(DestructEntityEvent.Death event, @Root Player player, @Getter("getTargetEntity") Monster monster) {
        Point.Builder p = newPoint("monster_death");
        addPlayer(p, player);
        EntityType type = monster.getType();
        p.tag("monster_type", type.getId());
        p.addField("monster_name", type.getName());
        addLocation(p, monster.getLocation());
        connection.write(p.build());
    }

}
