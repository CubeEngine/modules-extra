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
package org.cubeengine.module.observe.metrics.meter;

import org.cubeengine.module.observe.metrics.MetricCollection;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface Histogram {
    void observe(double seconds, Object... labels);

    abstract class Builder extends MeterBuilder<Histogram, Builder> {
        private double[] buckets;

        public Builder buckets(double... buckets) {
            this.buckets = buckets;
            return this;
        }

        @Override
        protected final @NotNull Histogram build(MetricCollection.Metadata metadata) {
            if (buckets == null || buckets.length == 0) {
                throw new IllegalStateException("buckets are required!");
            }
            return build(metadata, buckets);
        }

        protected abstract Histogram build(MetricCollection.Metadata metadata, double[] buckets);
    }
}
