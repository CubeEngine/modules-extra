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
package org.cubeengine.module.observe.metrics.pullgauge;

import io.prometheus.client.Collector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.emptyList;

public class PullGaugeCollector<T> extends Collector {
    private final T root;
    private final List<Function<T, MetricFamilySamples>> gauges;

    public PullGaugeCollector(T root, List<Function<T, MetricFamilySamples>> gauges) {
        this.root = root;
        this.gauges = gauges;
    }

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> samples = new ArrayList<>(gauges.size());

        for (Function<T, MetricFamilySamples> gauge : gauges) {
            samples.add(gauge.apply(root));
        }

        return samples;
    }

    public static <T> Builder<T> build(T root) {
        return new Builder<>(root, emptyList());
    }

    public static final class Builder<T> {
        private final T root;
        private final List<Function<T, MetricFamilySamples>> gauges;

        public Builder(T root, List<Function<T, MetricFamilySamples>> gauges) {
            this.root = root;
            this.gauges = gauges;
        }

        public Builder<T> withGauge(PullGauge<T> gauge) {
            return withGauge(gauge::sample);
        }

        public <V> Builder<T> withGauge(PullGauge<V> gauge, Function<T, V> selector) {
            return withGauge(root -> gauge.sample(selector.apply(root)));
        }

        public <V> Builder<T> withMultiGauge(PullGauge<V> gauge, Function<T, Iterable<V>> selector) {
            return withGauge(root -> gauge.sampleAll(selector.apply(root)));
        }

        public Builder<T> withGauge(Function<T, MetricFamilySamples> gauge) {
            final ArrayList<Function<T, MetricFamilySamples>> newGauges = new ArrayList<>(gauges.size() + 1);
            newGauges.addAll(gauges);
            newGauges.add(gauge);
            return new Builder<>(root, newGauges);
        }

        public PullGaugeCollector<T> build() {
            return new PullGaugeCollector<>(root, gauges);
        }
    }
}
