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
package org.cubeengine.module.observe;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import org.apache.logging.log4j.Logger;
import org.cubeengine.libcube.service.task.TaskManager;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
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

import static io.netty.channel.ChannelFutureListener.CLOSE;
import static io.netty.channel.ChannelFutureListener.CLOSE_ON_FAILURE;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.util.Arrays.asList;

public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest>
{
    private final Logger logger;
    private final CollectorRegistry syncRegistry;
    private final CollectorRegistry asyncRegistry;
    private final TaskManager tm;
    private final ByteBufAllocator allocator;

    public HttpRequestHandler(Logger logger, CollectorRegistry syncRegistry, CollectorRegistry asyncRegistry, TaskManager tm)
    {
        this.logger = logger;
        this.syncRegistry = syncRegistry;
        this.asyncRegistry = asyncRegistry;
        this.tm = tm;
        this.allocator = PooledByteBufAllocator.DEFAULT;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable t)
    {
        this.logger.error("An error occurred while processing an API request!", t);
        this.error(context, INTERNAL_SERVER_ERROR);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest message) throws Exception
    {
        final String path = message.uri();
        switch (path) {
            case "/health":
                handleHealthRequest(ctx, message);
                break;
            case "/metrics":
                handleMetricsRequest(ctx, message);
                break;
            default:
                logger.warn("Received request for {}", path);
                error(ctx, HttpResponseStatus.NOT_FOUND);
                break;
        }
    }

    private void handleHealthRequest(ChannelHandlerContext ctx, FullHttpRequest message) throws Exception {
        ctx.writeAndFlush(new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.OK)).addListener(CLOSE).addListener(CLOSE_ON_FAILURE);
    }

    private void handleMetricsRequest(ChannelHandlerContext ctx, FullHttpRequest message) {
        final String contentType = TextFormat.chooseContentType(message.headers().get(HttpHeaderNames.ACCEPT));
        final Set<String> query = parseQuery(message.uri());

        final CompletableFuture<Stream<Collector.MetricFamilySamples>> syncSamples = getSamples(syncRegistry, query, tm::runTask);

        final CompletableFuture<Stream<Collector.MetricFamilySamples>> asyncSamples = getSamples(asyncRegistry, query, tm::runTaskAsync);

        sequence(asList(syncSamples, asyncSamples))
                .thenAccept(allSamples -> writeMetricsResponse(ctx, contentType, allSamples.stream().reduce(Stream.empty(), Stream::concat)))
                .whenCompleteAsync((allSamples, t) -> {
                    if (t != null) {
                        logger.error("Failed to collect the samples!", t);
                        error(ctx, INTERNAL_SERVER_ERROR);
                    }
                });

    }

    private CompletableFuture<Stream<Collector.MetricFamilySamples>> getSamples(CollectorRegistry registry, Set<String> query, Consumer<Runnable> executor) {
        final CompletableFuture<Stream<Collector.MetricFamilySamples>> promise = new CompletableFuture<>();
        tm.runTaskAsyncDelayed(() -> promise.completeExceptionally(new TimeoutException()), Duration.ofSeconds(5));
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

    private void writeMetricsResponse(ChannelHandlerContext ctx, String contentType, Stream<Collector.MetricFamilySamples> samples) {
        final ByteBuf buffer = allocator.buffer();
        final OutputStreamWriter writer = new OutputStreamWriter(new ByteBufOutputStream(buffer), StandardCharsets.UTF_8);
        try {
            TextFormat.writeFormat(contentType, writer, streamAsEnumeration(samples));
            writer.flush();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write samples to response buffer!", e);
        }
        final DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.OK, buffer);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        ctx.writeAndFlush(response).addListener(CLOSE).addListener(CLOSE_ON_FAILURE);
    }

    private static Set<String> parseQuery(String uri) {
        final QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri);
        final List<String> names = queryStringDecoder.parameters().get("names[]");
        if (names == null || names.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(names);
    }

    private void error(ChannelHandlerContext context, HttpResponseStatus error)
    {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, error);
        context.writeAndFlush(response).addListener(CLOSE).addListener(CLOSE_ON_FAILURE);
    }

}
