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
package org.cubeengine.module.observe.health.impl;

import org.cubeengine.module.observe.health.AsyncHealthProbe;
import org.cubeengine.module.observe.health.HealthState;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.plugin.PluginContainer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

public class LastTickHealth implements AsyncHealthProbe {

    private final AtomicLong lastTicked = new AtomicLong(-1);
    private final long maximumTickDurationMillis;

    public LastTickHealth(PluginContainer plugin, Scheduler executorService, long maximumTickDurationMillis) {
        final Task task = Task.builder()
                .name("last-tick-health-check")
                .delay(Ticks.zero())
                .interval(Ticks.of(1))
                .plugin(plugin)
                .execute(this::tick)
                .build();
        executorService.submit(task);
        this.maximumTickDurationMillis = maximumTickDurationMillis;
    }

    private void tick() {
        lastTicked.set(System.currentTimeMillis());
    }

    @Override
    public CompletableFuture<HealthState> probe() {
        if (lastTicked.get() - System.currentTimeMillis() > maximumTickDurationMillis) {
            return CompletableFuture.completedFuture(HealthState.BROKEN);
        }
        return CompletableFuture.completedFuture(HealthState.HEALTHY);
    }
}
