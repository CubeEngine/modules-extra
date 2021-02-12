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
package org.cubeengine.module.observe.tracing.impl;

import io.jaegertracing.internal.metrics.Counter;
import io.jaegertracing.internal.metrics.Gauge;
import io.jaegertracing.internal.metrics.Timer;
import io.jaegertracing.spi.MetricsFactory;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Histogram;

import java.util.Map;

public class PrometheusMetricsFactory implements MetricsFactory {
    private final CollectorRegistry registry;

    public PrometheusMetricsFactory(CollectorRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Counter createCounter(String name, Map<String, String> tags) {
        return new Counter() {
            private final io.prometheus.client.Counter counter = new io.prometheus.client.Counter.Builder()
                    .name(name)
                    .labelNames(tags.keySet().toArray(new String[0]))
                    .register(registry);
            private final String[] labelValues = tags.values().toArray(new String[0]);

            @Override
            public void inc(long delta) {
                counter.labels(labelValues).inc(delta);
            }
        };
    }

    @Override
    public Timer createTimer(String name, Map<String, String> tags) {
        return new Timer() {
            private final io.prometheus.client.Histogram histogram = new Histogram.Builder()
                    .name(name)
                    .labelNames(tags.keySet().toArray(new String[0]))
                    .register(registry);
            private final String[] labelValues = tags.values().toArray(new String[0]);

            @Override
            public void durationMicros(long time) {
                histogram.labels(labelValues).observe(time);
            }
        };
    }

    @Override
    public Gauge createGauge(String name, Map<String, String> tags) {
        return new Gauge() {
            private final io.prometheus.client.Gauge gauge = new io.prometheus.client.Gauge.Builder()
                    .name(name)
                    .labelNames(tags.keySet().toArray(new String[0]))
                    .register(registry);
            private final String[] labelValues = tags.values().toArray(new String[0]);

            @Override
            public void update(long amount) {
                gauge.labels(labelValues).set(amount);
            }
        };
    }
}
