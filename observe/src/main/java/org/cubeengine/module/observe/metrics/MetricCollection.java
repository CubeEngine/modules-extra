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

import org.cubeengine.module.observe.metrics.meter.Counter;
import org.cubeengine.module.observe.metrics.meter.Gauge;
import org.cubeengine.module.observe.metrics.meter.Histogram;
import org.cubeengine.module.observe.metrics.meter.Timer;

import java.util.function.Function;

public interface MetricCollection {
    Counter newCounter(Metadata metadata);
    Counter.Builder newCounter();

    Gauge newGauge(Metadata metadata);
    Gauge.Builder newGauge();

    Timer newTimer(Metadata metadata);
    Timer.Builder newTimer();

    Histogram newHistogram(Metadata metadata, double[] buckets);
    Histogram.Builder newHistogram();

    void subscribe(MetricSubscriber subscriber);
    void unsubscribe(MetricSubscriber subscriber);

    static String[] name(String... segments) {
        return segments;
    }

    static MetricCollection newCollection() {
        return new SimpleMetricCollection();
    }

    class Metadata {
        private final String[] name;
        private final String help;
        private final String[] labelNames;

        public Metadata(String[] name, String help, String[] labelNames) {
            if (name.length == 0) {
                throw new IllegalArgumentException("at least one name segment is required!");
            }
            this.name = name;
            this.help = help;
            this.labelNames = labelNames;
        }

        public String[] getName() {
            return name;
        }

        public String getHelp() {
            return help;
        }

        public String[] getLabelNames() {
            return labelNames;
        }
    }

    class Builder<T> {
        private final String[] name;
        private final Function<Metadata, T> constructor;

        private String help;
        private String[] labelNames;

        public Builder(String[] name, Function<Metadata, T> constructor) {
            this.name = name;
            this.constructor = constructor;
        }

        public Builder<T> help(String help) {
            this.help = help;
            return this;
        }


        public Builder<T> labels(String... names) {
            this.labelNames = names;
            return this;
        }

        public T build() {
            if (help == null) {
                throw new IllegalStateException("help is required!");
            }
            if (labelNames == null) {
                throw new IllegalStateException("labels are required!");
            }

            Metadata metadata = new Metadata(name, help, labelNames);
            return constructor.apply(metadata);
        }
    }
}
