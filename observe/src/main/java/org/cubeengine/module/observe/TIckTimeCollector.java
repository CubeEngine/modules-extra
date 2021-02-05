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
package org.cubeengine.module.observe;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import org.cubeengine.libcube.service.task.TaskManager;
import org.spongepowered.api.Server;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.plugin.PluginContainer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class TIckTimeCollector extends Collector {
    private static Field TICK_TIMES_FIELD = null;

    static {
        try {
            Class<?> clazz = Class.forName("net.minecraft.server.MinecraftServer");
            TICK_TIMES_FIELD = clazz.getField("tickTimes");
        } catch (Exception e) {
            System.out.println("Failed to find timings field for additional internal value based metrics!");
            e.printStackTrace(System.out);
        }
    }

    private final Server server;
    private final Timing timing;

    private long maxTime = 0;
    private long minTime = 0;

    public TIckTimeCollector(Server server, TaskManager tm, PluginContainer plugin) {
        this.server = server;
        this.timing = Timings.of(plugin, "tick-time-collector");
        final long[] tickTimes = getTickTimes(server);
        if (tickTimes != null) {
            tm.runTimer(t -> updateTimes(), Ticks.of(tickTimes.length));
        }
    }

    private synchronized void updateTimes() {
        final long[] tickTimes = getTickTimes(server);
        if (tickTimes != null && tickTimes.length > 0) {
            long min = tickTimes[0];
            long max = tickTimes[0];
            long value;
            for (int i = 1; i < tickTimes.length; i++) {
                value = tickTimes[i];
                if (value > max) {
                    max = value;
                } else if (value < min) {
                    min = value;
                }
            }

            if (minTime == 0 || minTime > min) {
                minTime = min;
            }

            if (maxTime == 0 || maxTime < max) {
                maxTime = max;
            }
        }
    }

    @Override
    public synchronized List<MetricFamilySamples> collect() {
        try (Timing ignored = timing.startTiming()) {
            List<MetricFamilySamples> metrics = new ArrayList<>(2);
            if (minTime != 0) {
                metrics.add(new GaugeMetricFamily("sponge_server_min_tick_time_millis", "Minimum tick time over the last 100 ticks", minTime / 1e6));
                minTime = 0;
            }

            if (maxTime != 0) {
                metrics.add(new GaugeMetricFamily("sponge_server_max_tick_time_millis", "Maximum tick time over the last 100 ticks", maxTime / 1e6));
                maxTime = 0;
            }

            return metrics;
        }
    }

    private static long[] getTickTimes(Server server) {
        if (TICK_TIMES_FIELD == null) {
            return null;
        }
        try {
            return (long[]) TICK_TIMES_FIELD.get(server);
        } catch (Exception e) {
            return null;
        }
    }
}
