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
