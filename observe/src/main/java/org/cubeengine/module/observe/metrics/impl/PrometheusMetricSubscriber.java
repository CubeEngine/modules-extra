package org.cubeengine.module.observe.metrics.impl;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.SimpleCollector;
import io.prometheus.client.SimpleCollector.Builder;
import org.cubeengine.module.observe.metrics.MetricCollection;
import org.cubeengine.module.observe.metrics.MetricSubscriber;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PrometheusMetricSubscriber implements MetricSubscriber {
    private final CollectorRegistry registry;

    private final ConcurrentMap<String[], Counter> counterCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String[], Gauge> gaugeCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String[], Histogram> timerCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String[], Histogram> histogramCache = new ConcurrentHashMap<>();

    public PrometheusMetricSubscriber(CollectorRegistry registry) {
        this.registry = registry;
    }

    private <CC, C extends SimpleCollector<CC>, B extends Builder<B, C>> CC getLabeledCollector(MetricCollection.Metadata metadata,
                                                                                                Supplier<B> builder,
                                                                                                ConcurrentMap<String[], C> cache,
                                                                                                Object[] labelValues) {
        return getLabeledCollector(metadata, builder, cache, labelValues, ignored -> {});
    }

    private <ChildT, CollectorT extends SimpleCollector<ChildT>, BuilderT extends Builder<BuilderT, CollectorT>> ChildT getLabeledCollector(MetricCollection.Metadata metadata,
                                                                                                                                            Supplier<BuilderT> builder,
                                                                                                                                            ConcurrentMap<String[], CollectorT> cache,
                                                                                                                                            Object[] labelValues,
                                                                                                                                            Consumer<BuilderT> customizer) {
        final CollectorT collector = cache.computeIfAbsent(metadata.getName(), name -> {
            final BuilderT instance = builder.get();
            instance.name(String.join("_", name))
                    .help(metadata.getHelp())
                    .labelNames(metadata.getLabelNames());
            customizer.accept(instance);
            return instance.register(registry);
        });
        System.out.println("You got foobared");

        String[] labelValueStrings = new String[labelValues.length];
        for (int i = 0; i < labelValues.length; i++) {
            labelValueStrings[i] = String.valueOf(labelValues[i]);
        }
        return collector.labels(labelValueStrings);
    }

    @Override
    public void onCounterIncrement(MetricCollection.Metadata metadata, double incrementedBy, Object[] labelValues) {
        getLabeledCollector(metadata, Counter::build, counterCache, labelValues).inc(incrementedBy);
    }

    @Override
    public void onGaugeSet(MetricCollection.Metadata metadata, double value, Object[] labelValues) {
        getLabeledCollector(metadata, Gauge::build, gaugeCache, labelValues).set(value);
    }

    @Override
    public void onTimerObserved(MetricCollection.Metadata metadata, double seconds, Object[] labelValues) {
        getLabeledCollector(metadata, Histogram::build, timerCache, labelValues).observe(seconds);
    }

    @Override
    public void onHistogramObserved(MetricCollection.Metadata metadata, double[] buckets, double value, Object[] labelValues) {
        getLabeledCollector(metadata, Histogram::build, histogramCache, labelValues, b -> b.buckets(buckets)).observe(value);
    }
}
