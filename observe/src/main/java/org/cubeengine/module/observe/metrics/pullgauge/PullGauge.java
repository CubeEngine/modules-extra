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

import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import org.cubeengine.module.observe.LabeledPullGauge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.ToDoubleFunction;

import static io.prometheus.client.Collector.Type.GAUGE;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public abstract class PullGauge<T> {
    private final String name;
    private final String unit;
    private final String help;

    public PullGauge(String name, String unit, String help) {
        this.name = name;
        this.unit = unit;
        this.help = help;
    }

    protected final Sample sample(List<String> labelNames, List<String> labelValues, double value) {
        return new Sample(name, labelNames, labelValues, value);
    }

    protected final Sample sample(double value) {
        return sample(emptyList(), emptyList(), value);
    }

    protected final MetricFamilySamples bundle(List<Sample> samples) {
        return new MetricFamilySamples(name, unit, GAUGE, help, samples);
    }

    protected abstract List<Sample> makeSamples(T subject);

    public final MetricFamilySamples sample(T subject) {
        return bundle(makeSamples(subject));
    }

    public final MetricFamilySamples sampleAll(Iterable<T> subjects) {
        List<Sample> samples;
        if (subjects instanceof Collection) {
            samples = new ArrayList<>(((Collection<T>) subjects).size());
        } else {
            samples = new ArrayList<>();
        }

        for (T subject : subjects) {
            samples.addAll(makeSamples(subject));
        }

        return bundle(samples);
    }

    public static <T> Builder<T> build(String name, ToDoubleFunction<T> f) {
        return new Builder<>("", "", (unit, help) -> new SimplePullGauge<>(name, unit, help, f));
    }

    public static <T> Builder<T> build(String name, LabeledPullGauge.Sampler<T> f) {
        return new Builder<>("", "", (unit, help) -> new LabeledPullGauge<>(name, unit, help, v -> singletonList(f.apply(v))));
    }

    public static <T> Builder<T> build(String name, LabeledPullGauge.MultiSampler<T> f) {
        return new Builder<>("", "", (unit, help) -> new LabeledPullGauge<>(name, unit, help, f));
    }

    public static final class Builder<T> {
        private final String unit;
        private final String help;
        private final BiFunction<String, String, PullGauge<T>> ctor;

        private Builder(String unit, String help, BiFunction<String, String, PullGauge<T>> ctor) {
            this.unit = unit;
            this.help = help;
            this.ctor = ctor;
        }

        public Builder<T> unit(String unit) {
            return new Builder<>(unit, help, ctor);
        }

        public Builder<T> help(String help) {
            return new Builder<>(unit, help, ctor);
        }

        public PullGauge<T> build() {
            return ctor.apply(unit, help);
        }
    }

    public static final class LabeledValue {
        private final double value;
        private final Iterable<Label> labels;

        public LabeledValue(double value, Iterable<Label> labels) {
            this.value = value;
            this.labels = labels;
        }

        public double getValue() {
            return value;
        }

        public Iterable<Label> getLabels() {
            return labels;
        }

        public static LabeledValue value(double value, Label... labels) {
            return value(value, Arrays.asList(labels));
        }

        public static LabeledValue value(double value, List<Label> labels) {
            return new LabeledValue(value, labels);
        }
    }

    public static final class Label {
        private final String name;
        private final String value;

        public Label(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public static Label label(String name, String value) {
            return new Label(name, value);
        }
    }
}
