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
package org.cubeengine.module.observe.metrics.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import org.apache.logging.log4j.Logger;
import org.cubeengine.module.observe.web.FailureCallback;
import org.cubeengine.module.observe.web.WebHandler;
import org.cubeengine.module.observe.web.SuccessCallback;
import org.cubeengine.module.observe.metrics.MetricsService;
import org.spongepowered.plugin.PluginContainer;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Stream;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.cubeengine.module.observe.Util.*;

public class PrometheusMetricsService implements MetricsService, WebHandler
{
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final List<Registration> syncRegistries;
    private final List<Registration> asyncRegistries;
    private final ExecutorService syncExecutor;
    private final ScheduledExecutorService asyncExecutor;
    private final Logger logger;

    public PrometheusMetricsService(ExecutorService syncExecutor, ScheduledExecutorService asyncExecutor, Logger logger)
    {
        this.syncExecutor = syncExecutor;
        this.asyncExecutor = asyncExecutor;
        this.syncRegistries = new ArrayList<>();
        this.asyncRegistries = new ArrayList<>();
        this.logger = logger;
    }

    @Override
    public synchronized void addCollectorRegistry(PluginContainer plugin, CollectorRegistry registry, boolean asyncCapable) {
        final Registration registration = new Registration(plugin, registry);
        if (asyncCapable) {
            asyncRegistries.add(registration);
        } else {
            syncRegistries.add(registration);
        }
    }

    @Override
    public synchronized void removeCollectorRegistry(PluginContainer plugin, CollectorRegistry registry) {
        syncRegistries.removeIf(r -> r.getPlugin().equals(plugin) && r.getRegistry().equals(registry));
        asyncRegistries.removeIf(r -> r.getPlugin().equals(plugin) && r.getRegistry().equals(registry));
    }

    @Override
    public synchronized void removeAllCollectorRegistries(PluginContainer plugin) {
        syncRegistries.removeIf(r -> r.getPlugin().equals(plugin));
        asyncRegistries.removeIf(r -> r.getPlugin().equals(plugin));
    }

    public void handleRequest(SuccessCallback success, FailureCallback failure, FullHttpRequest message, QueryStringDecoder queryStringDecoder) {
        final String contentType = TextFormat.chooseContentType(message.headers().get(HttpHeaderNames.ACCEPT));
        final Set<String> query = parseQuery(queryStringDecoder);

        final List<Registration> syncRegistries;
        final List<Registration> asyncRegistries;
        synchronized (this) {
            syncRegistries = new ArrayList<>(this.syncRegistries);
            asyncRegistries = new ArrayList<>(this.asyncRegistries);
        }

        final CompletableFuture<Stream<Collector.MetricFamilySamples>> syncSamples = getSyncSamples(syncRegistries, query);
        final CompletableFuture<Stream<Collector.MetricFamilySamples>> asyncSamples = getAsyncSamples(asyncRegistries, query);

        syncSamples.thenComposeAsync((sync) -> asyncSamples.thenApply((async) -> Stream.concat(sync, async)))
                .whenComplete((allSamples, t) -> {
                    if (t != null) {
                        logger.error("Failed to collect the samples!", t);
                        failure.fail(INTERNAL_SERVER_ERROR, t);
                    } else {
                        writeMetricsResponse(success, failure, contentType, allSamples, message.content().alloc());
                    }
                });

    }

    private static void eagerScrapeInto(CollectorRegistry registry, Set<String> query, List<Collector.MetricFamilySamples> output) {
        final Enumeration<Collector.MetricFamilySamples> enumeration = registry.filteredMetricFamilySamples(query);
        while (enumeration.hasMoreElements()) {
            output.add(enumeration.nextElement());
        }
    }

    private CompletableFuture<Stream<Collector.MetricFamilySamples>> getSyncSamples(Collection<Registration> registrations, Set<String> query) {

        return CompletableFuture.supplyAsync(() -> {
            List<Collector.MetricFamilySamples> samples = new ArrayList<>();
            for (Registration registration : registrations) {
                eagerScrapeInto(registration.getRegistry(), query, samples);
            }
            return samples.stream();
        }, syncExecutor);
    }

    private static CompletableFuture<Stream<Collector.MetricFamilySamples>> getAsyncSamples(List<Registration> registrations, Set<String> query) {
        final Stream<Collector.MetricFamilySamples> defaultSamples = enumerationAsStream(CollectorRegistry.defaultRegistry.filteredMetricFamilySamples(query));
        final Stream<Collector.MetricFamilySamples> pluginSamples = registrations.stream()
                .parallel()
                .flatMap(registration -> enumerationAsStream(registration.getRegistry().filteredMetricFamilySamples(query)));
        return CompletableFuture.completedFuture(Stream.concat(defaultSamples, pluginSamples));
    }

    private <T> CompletableFuture<T> timeout(CompletableFuture<T> promise) {
        return race(promise, timeoutAfter(TIMEOUT, asyncExecutor));
    }

    private void writeMetricsResponse(SuccessCallback success, FailureCallback failure, String contentType, Stream<Collector.MetricFamilySamples> samples, ByteBufAllocator allocator) {
        final ByteBuf buffer = allocator.buffer();
        final OutputStreamWriter writer = new OutputStreamWriter(new ByteBufOutputStream(buffer), StandardCharsets.UTF_8);
        try {
            TextFormat.writeFormat(contentType, writer, streamAsEnumeration(samples));
            writer.flush();
        } catch (IOException e) {
            failure.fail(INTERNAL_SERVER_ERROR, e);
            return;
        }
        final DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.OK, buffer);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        success.succeed(response);
    }

    public static <T> Enumeration<T> streamAsEnumeration(Stream<T> s) {
        final Iterator<T> iterator = s.iterator();
        return new Enumeration<T>() {
            @Override
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @Override
            public T nextElement() {
                return iterator.next();
            }
        };
    }

    private static Set<String> parseQuery(QueryStringDecoder queryStringDecoder) {
        final List<String> names = queryStringDecoder.parameters().get("names[]");
        if (names == null || names.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(names);
    }

    private static final class Registration {
        private final PluginContainer plugin;
        private final CollectorRegistry registry;

        public Registration(PluginContainer plugin, CollectorRegistry registry) {
            this.plugin = plugin;
            this.registry = registry;
        }

        public PluginContainer getPlugin() {
            return plugin;
        }

        public CollectorRegistry getRegistry() {
            return registry;
        }
    }
}
