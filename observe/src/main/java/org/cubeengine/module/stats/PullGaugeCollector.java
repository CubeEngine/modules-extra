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

import io.prometheus.client.Collector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.emptyList;

public class PullGaugeCollector<T> extends Collector {
    private final T root;
    private final List<Gauge<T>> gauges;

    public PullGaugeCollector(T root, List<Gauge<T>> gauges) {
        this.root = root;
        this.gauges = gauges;
    }

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> samples = new ArrayList<>(gauges.size());

        for (Gauge<T> gauge : gauges) {
            samples.add(gauge.sample(root));
        }

        return samples;
    }

    public static <T> Builder<T> build(T root) {
        return new Builder<>(root, emptyList());
    }

    private interface Gauge<T> {
        MetricFamilySamples sample(T root);
    }

    private static final class DirectGauge<T> implements Gauge<T> {
        private final PullGauge<T> gauge;

        public DirectGauge(PullGauge<T> gauge) {
            this.gauge = gauge;
        }

        public MetricFamilySamples sample(T root) {
            return gauge.sample(root);
        }
    }

    private static final class SingleIndirectGauge<T, V> implements Gauge<T> {
        private final Function<T, V> selector;
        private final PullGauge<V> gauge;

        public SingleIndirectGauge(Function<T, V> selector, PullGauge<V> gauge) {
            this.selector = selector;
            this.gauge = gauge;
        }

        public MetricFamilySamples sample(T root) {
            return gauge.sample(selector.apply(root));
        }
    }

    private static final class MultiIndirectGauge<T, V> implements Gauge<T> {
        private final Function<T, Iterable<V>> selector;
        private final PullGauge<V> gauge;

        public MultiIndirectGauge(Function<T, Iterable<V>> selector, PullGauge<V> gauge) {
            this.selector = selector;
            this.gauge = gauge;
        }

        public MetricFamilySamples sample(T root) {
            return gauge.sampleAll(selector.apply(root));
        }
    }

    public static final class Builder<T> {
        private final T root;
        private final List<Gauge<T>> gauges;

        public Builder(T root, List<Gauge<T>> gauges) {
            this.root = root;
            this.gauges = gauges;
        }

        public Builder<T> withGauge(PullGauge<T> gauge) {
            return withGauge(new DirectGauge<>(gauge));
        }

        public <V> Builder<T> withGauge(PullGauge<V> gauge, Function<T, V> selector) {
            return withGauge(new SingleIndirectGauge<>(selector, gauge));
        }

        public <V> Builder<T> withMultiGauge(PullGauge<V> gauge, Function<T, Iterable<V>> selector) {
            return withGauge(new MultiIndirectGauge<>(selector, gauge));
        }

        private Builder<T> withGauge(Gauge<T> gauge) {
            final ArrayList<Gauge<T>> newGauges = new ArrayList<>(gauges.size() + 1);
            newGauges.addAll(gauges);
            newGauges.add(gauge);
            return new Builder<>(root, newGauges);
        }

        public PullGaugeCollector<T> build() {
            return new PullGaugeCollector<>(root, gauges);
        }
    }
}
