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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class SimpleMetricCollection implements MetricCollection {

    private final List<MetricSubscriber> subscribers = new CopyOnWriteArrayList<>();

    @Override
    public void subscribe(MetricSubscriber subscriber) {
        subscribers.add(subscriber);
    }

    @Override
    public void unsubscribe(MetricSubscriber subscriber) {
        subscribers.remove(subscriber);
    }

    @Override
    public Counter newCounter(String[] name, String help, String... labelNames) {
        final Metadata metadata = new Metadata(name, help, labelNames);
        return (by, labels) -> forAll(s -> s.onCounterIncrement(metadata, by, labels));
    }

    @Override
    public Gauge newGauge(String[] name, String help, String... labelNames) {
        final Metadata metadata = new Metadata(name, help, labelNames);
        return (value, labels) -> forAll(s -> s.onGaugeSet(metadata, value, labels));
    }

    @Override
    public Timer newTimer(String[] name, String help, String... labelNames) {
        final Metadata metadata = new Metadata(name, help, labelNames);
        return (seconds, labels) -> forAll(s -> s.onTimerObserved(metadata, seconds, labels));
    }

    @Override
    public Histogram newHistogram(String[] name, String help, double[] buckets, String... labelNames) {
        final Metadata metadata = new Metadata(name, help, labelNames);
        return (value, labels) -> forAll(s -> s.onHistogramObserved(metadata, buckets, value, labels));
    }

    private void forAll(Consumer<MetricSubscriber> f) {
        subscribers.forEach(f);
    }

}
