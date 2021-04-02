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

public interface MetricCollection {
    Counter newCounter(String[] name, String help, String... labelNames);
    Gauge newGauge(String[] name, String help, String... labelNames);
    Timer newTimer(String[] name, String help, String... labelNames);
    Histogram newHistogram(String[] name, String help, double[] buckets, String... labelNames);

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

        Metadata(String[] name, String help, String[] labelNames) {
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
}
