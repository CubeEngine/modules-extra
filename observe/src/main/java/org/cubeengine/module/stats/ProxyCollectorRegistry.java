package org.cubeengine.module.stats;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;

import java.util.Enumeration;
import java.util.Set;

public class ProxyCollectorRegistry extends CollectorRegistry {
    private final CollectorRegistry registry;


    public ProxyCollectorRegistry(CollectorRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void register(Collector m) {
        registry.register(m);
    }

    @Override
    public void unregister(Collector m) {
        registry.unregister(m);
    }

    @Override
    public void clear() {
        registry.clear();
    }

    @Override
    public Enumeration<Collector.MetricFamilySamples> metricFamilySamples() {
        return registry.metricFamilySamples();
    }

    @Override
    public Enumeration<Collector.MetricFamilySamples> filteredMetricFamilySamples(Set<String> includedNames) {
        return registry.filteredMetricFamilySamples(includedNames);
    }

    @Override
    public Double getSampleValue(String name) {
        return registry.getSampleValue(name);
    }

    @Override
    public Double getSampleValue(String name, String[] labelNames, String[] labelValues) {
        return registry.getSampleValue(name, labelNames, labelValues);
    }
}
