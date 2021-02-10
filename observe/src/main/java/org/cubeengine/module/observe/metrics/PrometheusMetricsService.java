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
package org.cubeengine.module.observe.metrics;

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
import org.cubeengine.libcube.service.task.TaskManager;
import org.cubeengine.module.observe.FailureCallback;
import org.cubeengine.module.observe.SuccessCallback;
import org.cubeengine.module.observe.WebServer;
import org.spongepowered.plugin.PluginContainer;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.util.Arrays.asList;

public class PrometheusMetricsService implements MetricsService
{
    private final CollectorRegistry syncRegistry;
    private final CollectorRegistry asyncRegistry;
    private final TaskManager tm;
    private final Logger logger;

    public PrometheusMetricsService(TaskManager tm, WebServer server, Logger logger)
    {
        this.tm = tm;
        this.logger = logger;
        this.syncRegistry = new CollectorRegistry();
        this.asyncRegistry = new CollectorRegistry();
        server.registerHandler("/metrics", this::handleMetricsRequest);
    }

    public synchronized void registerCollector(PluginContainer plugin, SyncCollector collector)
    {
        this.syncRegistry.register(collector);
    }

    public synchronized void registerCollector(PluginContainer plugin, Collector collector)
    {
        this.asyncRegistry.register(collector);
    }

    @Override
    public void unregisterCollector(PluginContainer plugin, SyncCollector collector) {
        this.syncRegistry.unregister(collector);
    }

    @Override
    public void unregisterCollector(PluginContainer plugin, Collector collector) {
        this.asyncRegistry.unregister(collector);
    }

    @Override
    public void unregisterCollectors(PluginContainer plugin) {
        // TODO implement me
    }

    private void handleMetricsRequest(SuccessCallback success, FailureCallback failure, FullHttpRequest message, QueryStringDecoder queryStringDecoder) {
        final String contentType = TextFormat.chooseContentType(message.headers().get(HttpHeaderNames.ACCEPT));
        final Set<String> query = parseQuery(queryStringDecoder);

        final CompletableFuture<Stream<Collector.MetricFamilySamples>> syncSamples = getSamples(syncRegistry, query, tm::runTask);

        final CompletableFuture<Stream<Collector.MetricFamilySamples>> asyncSamples = getSamples(asyncRegistry, query, tm::runTaskAsync);

        sequence(asList(syncSamples, asyncSamples))
                .thenAccept(allSamples -> writeMetricsResponse(success, failure, contentType, allSamples.stream().reduce(Stream.empty(), Stream::concat), message.content().alloc()))
                .whenCompleteAsync((allSamples, t) -> {
                    if (t != null) {
                        logger.error("Failed to collect the samples!", t);
                        failure.fail(INTERNAL_SERVER_ERROR, t);
                    }
                });

    }

    private CompletableFuture<Stream<Collector.MetricFamilySamples>> getSamples(CollectorRegistry registry, Set<String> query, Consumer<Runnable> executor) {
        final CompletableFuture<Stream<Collector.MetricFamilySamples>> promise = new CompletableFuture<>();
        executor.accept(() -> {
            try {
                List<Collector.MetricFamilySamples> samples = new ArrayList<>();
                final Enumeration<Collector.MetricFamilySamples> enumeration = registry.filteredMetricFamilySamples(query);
                while (enumeration.hasMoreElements()) {
                    samples.add(enumeration.nextElement());
                }
                promise.complete(samples.stream());
            } catch (Exception e) {
                promise.completeExceptionally(e);
            }
        });
        return promise;
    }


    public <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> com) {

        CompletableFuture<List<T>> identity = CompletableFuture.completedFuture(new ArrayList<>(com.size()));

        BiFunction<CompletableFuture<List<T>>, CompletableFuture<T>, CompletableFuture<List<T>>> combineToList = (acc, next) -> acc.thenCombine(next, (a, b) -> {
            a.add(b);
            return a;
        });

        BinaryOperator<CompletableFuture<List<T>>> combineLists = (a, b) -> a.thenCombine(b, (l1, l2) -> {
            l1.addAll(l2);
            return l1;
        });

        return com.stream().reduce(identity, combineToList, combineLists);

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

    private <T> CompletableFuture<T> timeoutAfter(Duration duration) {
        CompletableFuture<T> promise = new CompletableFuture<>();
        tm.runTaskAsyncDelayed(() -> promise.completeExceptionally(new TimeoutException()), duration);
        return promise;
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