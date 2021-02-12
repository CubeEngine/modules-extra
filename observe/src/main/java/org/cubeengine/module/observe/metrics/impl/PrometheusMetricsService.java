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
import org.cubeengine.module.observe.metrics.SyncCollector;
import org.spongepowered.plugin.PluginContainer;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Stream;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.util.Arrays.asList;
import static java.util.Collections.newSetFromMap;
import static org.cubeengine.module.observe.Util.*;

public class PrometheusMetricsService implements MetricsService, WebHandler
{
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final CollectorRegistry syncRegistry = new CollectorRegistry();
    private final CollectorRegistry asyncRegistry;
    private final Map<PluginContainer, Set<Collector>> collectorsPerPlugins = new HashMap<>();
    private final ExecutorService syncExecutor;
    private final ScheduledExecutorService asyncExecutor;
    private final Logger logger;

    public PrometheusMetricsService(ExecutorService syncExecutor, ScheduledExecutorService asyncExecutor, CollectorRegistry asyncCollectorRegistry, Logger logger)
    {
        this.syncExecutor = syncExecutor;
        this.asyncExecutor = asyncExecutor;
        this.asyncRegistry = asyncCollectorRegistry;
        this.logger = logger;
    }

    public synchronized void registerCollector(PluginContainer plugin, SyncCollector collector)
    {
        this.syncRegistry.register(collector);
        trackCollector(plugin, collector);
    }

    public synchronized void registerCollector(PluginContainer plugin, Collector collector)
    {
        this.asyncRegistry.register(collector);
        trackCollector(plugin, collector);
    }

    private void trackCollector(PluginContainer plugin, Collector collector) {
        this.collectorsPerPlugins.computeIfAbsent(plugin, ignored -> newSetFromMap(new IdentityHashMap<>())).add(collector);
    }

    @Override
    public synchronized void unregisterCollector(PluginContainer plugin, SyncCollector collector) {
        this.syncRegistry.unregister(collector);
        forgetCollector(plugin, collector);
    }

    @Override
    public synchronized void unregisterCollector(PluginContainer plugin, Collector collector) {
        this.asyncRegistry.unregister(collector);
        forgetCollector(plugin, collector);
    }

    private void forgetCollector(PluginContainer plugin, Collector collector) {
        final Set<Collector> collectors = this.collectorsPerPlugins.get(plugin);
        if (collectors != null) {
            collectors.remove(collector);
        }
    }

    @Override
    public void unregisterCollectors(PluginContainer plugin) {
        final Set<Collector> collectors = this.collectorsPerPlugins.remove(plugin);
        if (collectors != null) {
            for (Collector collector : collectors) {
                this.syncRegistry.unregister(collector);
                this.asyncRegistry.unregister(collector);
            }
        }
    }

    public void handleRequest(SuccessCallback success, FailureCallback failure, FullHttpRequest message, QueryStringDecoder queryStringDecoder) {
        final String contentType = TextFormat.chooseContentType(message.headers().get(HttpHeaderNames.ACCEPT));
        final Set<String> query = parseQuery(queryStringDecoder);

        final CompletableFuture<Stream<Collector.MetricFamilySamples>> syncSamples = getSamples(syncRegistry, query, syncExecutor);
        final CompletableFuture<Stream<Collector.MetricFamilySamples>> asyncSamples = getSamples(asyncRegistry, query, asyncExecutor);

        sequence(asList(syncSamples, asyncSamples))
                .thenAccept(allSamples -> writeMetricsResponse(success, failure, contentType, allSamples.stream().reduce(Stream.empty(), Stream::concat), message.content().alloc()))
                .whenComplete((allSamples, t) -> {
                    if (t != null) {
                        logger.error("Failed to collect the samples!", t);
                        failure.fail(INTERNAL_SERVER_ERROR, t);
                    }
                });

    }

    private CompletableFuture<Stream<Collector.MetricFamilySamples>> getSamples(CollectorRegistry registry, Set<String> query, ExecutorService executor) {
        return CompletableFuture.supplyAsync(() -> {
            List<Collector.MetricFamilySamples> samples = new ArrayList<>();
            final Enumeration<Collector.MetricFamilySamples> enumeration = registry.filteredMetricFamilySamples(query);
            while (enumeration.hasMoreElements()) {
                samples.add(enumeration.nextElement());
            }
            return samples.stream();
        }, executor);
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

}
