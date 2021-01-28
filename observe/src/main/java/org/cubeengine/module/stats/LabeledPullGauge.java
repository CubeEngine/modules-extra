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
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

final class LabeledPullGauge<T> extends PullGauge<T> {
    private final Function<T, LabeledValue> f;

    public LabeledPullGauge(String name, String unit, String help, Function<T, LabeledValue> f) {
        super(name, unit, help);
        this.f = f;
    }

    @Override
    protected Collector.MetricFamilySamples.Sample makeSample(T subject) {
        final LabeledValue labeledValue = f.apply(subject);
        final Iterable<Label> labels = labeledValue.getLabels();
        final List<String> labelNames;
        final List<String> labelValues;
        if (labels instanceof Collection) {
            final int size = ((Collection<Label>) labels).size();
            labelNames = new ArrayList<>(size);
            labelValues = new ArrayList<>(size);
        } else {
            labelNames = new ArrayList<>();
            labelValues = new ArrayList<>();
        }
        for (Label label : labels) {
            labelNames.add(label.getName());
            labelValues.add(label.getValue());
        }
        return sample(labelNames, labelValues, labeledValue.getValue());
    }
}
