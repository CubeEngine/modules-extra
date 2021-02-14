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
package org.cubeengine.module.observe.tracing;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class InstrumentedExecutorService implements ExecutorService {
    private final TracingService tracingService;
    private final ExecutorService service;
    private final String label;

    public InstrumentedExecutorService(TracingService tracingService, ExecutorService service, String label) {
        this.tracingService = tracingService;
        this.service = service;
        this.label = label;
    }

    @Override
    public void shutdown() {
        service.shutdown();
    }

    @NotNull
    @Override
    public List<Runnable> shutdownNow() {
        return service.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return service.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return service.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        return service.awaitTermination(timeout, unit);
    }

    @NotNull
    @Override
    public <T> Future<T> submit(@NotNull Callable<T> task) {
        return service.submit(tracingService.instrument(label, task));
    }

    @NotNull
    @Override
    public <T> Future<T> submit(@NotNull Runnable task, T result) {
        return service.submit(tracingService.instrument(label, task), result);
    }

    @NotNull
    @Override
    public Future<?> submit(@NotNull Runnable task) {
        return service.submit(tracingService.instrument(label, task));
    }

    @NotNull
    @Override
    public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException {
        List<Callable<T>> instrumentedTasks = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            instrumentedTasks.add(tracingService.instrument(label, task));
        }
        return service.invokeAll(instrumentedTasks);
    }

    @NotNull
    @Override
    public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        List<Callable<T>> instrumentedTasks = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            instrumentedTasks.add(tracingService.instrument(label, task));
        }
        return service.invokeAll(instrumentedTasks, timeout, unit);
    }

    @NotNull
    @Override
    public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        List<Callable<T>> instrumentedTasks = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            instrumentedTasks.add(tracingService.instrument(label, task));
        }
        return service.invokeAny(instrumentedTasks);
    }

    @Override
    public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        List<Callable<T>> instrumentedTasks = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            instrumentedTasks.add(tracingService.instrument(label, task));
        }
        return service.invokeAny(instrumentedTasks);
    }

    @Override
    public void execute(@NotNull Runnable command) {
        service.execute(tracingService.instrument(label, command));
    }
}
