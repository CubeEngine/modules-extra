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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.cubeengine.module.observe.metrics.MetricCollection;
import org.spongepowered.api.util.Builder;

@SuppressWarnings("unchecked")
public abstract class MeterBuilder<T, B extends MeterBuilder<T, B>> implements Builder<T, B> {

    private String[] name;
    private String help;
    private String[] labelNames;

    public B help() {
        this.help = help;
        return (B) this;
    }

    public B labelNames(String... names) {
        this.labelNames = names;
        return (B) this;
    }

    protected abstract @NonNull T build(MetricCollection.Metadata metadata);

    @Override
    public final @NonNull T build() {
        if (name == null || name.length == 0) {
            throw new IllegalStateException("name is required!");
        }
        if (help == null) {
            throw new IllegalStateException("help is required!");
        }
        if (labelNames == null) {
            throw new IllegalStateException("labels are required!");
        }

        return build(new MetricCollection.Metadata(name, help, labelNames));
    }
}
