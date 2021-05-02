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
package org.cubeengine.module.observe.health.impl;

import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.cubeengine.libcube.util.Pair;
import org.cubeengine.module.observe.web.FailureCallback;
import org.cubeengine.module.observe.web.WebHandler;
import org.cubeengine.module.observe.web.SuccessCallback;
import org.cubeengine.module.observe.health.AsyncHealthProbe;
import org.cubeengine.module.observe.health.HealthCheckService;
import org.cubeengine.module.observe.health.HealthState;
import org.cubeengine.module.observe.health.SyncHealthProbe;
import org.spongepowered.plugin.PluginContainer;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.cubeengine.module.observe.Util.sequence;

public class SimpleHealthCheckService implements HealthCheckService, WebHandler {

    private final ExecutorService syncExecutor;

    private final Gson gson = new Gson();

    private final Map<String, SyncHealthProbe> syncProbes = new HashMap<>();
    private final Map<String, AsyncHealthProbe> asyncProbes = new HashMap<>();

    public SimpleHealthCheckService(ExecutorService syncExecutor) {
        this.syncExecutor = syncExecutor;
    }

    public void handleRequest(SuccessCallback success, FailureCallback failure, FullHttpRequest request, QueryStringDecoder queryStringDecoder) {
        sequence(asList(getSyncStates(), getAsyncStates())).whenComplete((results, t) -> {
            if (t != null) {
                failure.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR, t);
                return;
            }

            Map<String, HealthState> details = new HashMap<>();
            for (Map<String, HealthState> result : results) {
                details.putAll(result);
            }

            HealthState state = HealthState.HEALTHY;
            for (HealthState value : details.values()) {
                if (value.ordinal() > state.ordinal()) {
                    state = value;
                }
            }
            final HttpResponseStatus httpStatus = state != HealthState.HEALTHY ? HttpResponseStatus.SERVICE_UNAVAILABLE :  HttpResponseStatus.OK;
            final ByteBuf buffer = request.content().alloc().buffer();
            buffer.writeCharSequence(gson.toJson(new HealthResult(state, details)), StandardCharsets.UTF_8);
            final DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, httpStatus, buffer);
            response.headers().set("Content-Type", "application/json");
            success.succeed(response);
        });
    }

    private CompletableFuture<Map<String, HealthState>> getSyncStates() {
        final Map<String, SyncHealthProbe> probes;
        synchronized (this) {
            if (syncProbes.isEmpty()) {
                return CompletableFuture.completedFuture(Collections.emptyMap());
            }
            probes = new HashMap<>(this.syncProbes);
        }

        return CompletableFuture.supplyAsync(() -> {
            Map<String, HealthState> result = new HashMap<>(probes.size());
            for (Map.Entry<String, SyncHealthProbe> entry : probes.entrySet()) {
                result.put(entry.getKey(), entry.getValue().probe());
            }
            return result;
        }, syncExecutor);
    }

    private CompletableFuture<Map<String, HealthState>> getAsyncStates() {
        final Map<String, AsyncHealthProbe> probes;
        synchronized (this) {
            if (this.asyncProbes.isEmpty()) {
                return CompletableFuture.completedFuture(Collections.emptyMap());
            }
            probes = new HashMap<>(this.asyncProbes);
        }

        return sequence(probes.entrySet().stream().parallel().map(entry -> entry.getValue().probe().thenApply(r -> new Pair<>(entry.getKey(), r))).collect(toList()))
            .thenApply(results -> {
                final Map<String, HealthState> result = new HashMap<>(results.size());
                for (Pair<String, HealthState> pair : results) {
                    result.put(pair.getLeft(), pair.getRight());
                }
                return result;
            });
    }

    private static String prefix(PluginContainer plugin) {
        return plugin.metadata().id() + ":";
    }

    private static String identifier(PluginContainer plugin, String name) {
        return prefix(plugin) + name;
    }

    @Override
    public synchronized void registerProbe(PluginContainer plugin, String id, SyncHealthProbe probe) {
        String fullId = identifier(plugin, id);
        asyncProbes.remove(fullId);
        syncProbes.putIfAbsent(fullId, probe);
    }

    @Override
    public synchronized void registerProbe(PluginContainer plugin, String id, AsyncHealthProbe probe) {
        String fullId = identifier(plugin, id);
        syncProbes.remove(fullId);
        asyncProbes.putIfAbsent(identifier(plugin, id), probe);
    }

    @Override
    public synchronized void unregisterProbe(PluginContainer plugin, String id) {
        String fullId = identifier(plugin, id);
        syncProbes.remove(fullId);
        asyncProbes.remove(fullId);
    }

    @Override
    public synchronized void unregisterProbes(PluginContainer plugin) {
        String prefix = prefix(plugin);
        removeByPrefix(syncProbes, prefix);
        removeByPrefix(asyncProbes, prefix);
    }

    private static void removeByPrefix(Map<String, ?> map, String prefix) {
        map.entrySet().removeIf(entry -> entry.getKey().startsWith(prefix));
    }
}
