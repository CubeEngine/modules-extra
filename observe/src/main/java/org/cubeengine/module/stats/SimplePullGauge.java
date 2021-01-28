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

import java.util.function.ToDoubleFunction;

import static java.util.Collections.emptyList;

final class SimplePullGauge<T> extends PullGauge<T> {
    private final ToDoubleFunction<T> f;

    public SimplePullGauge(String name, String unit, String help, ToDoubleFunction<T> f) {
        super(name, unit, help);
        this.f = f;
    }

    @Override
    protected Collector.MetricFamilySamples.Sample makeSample(T subject) {
        return sample(f.applyAsDouble(subject));
    }
}
