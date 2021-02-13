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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public abstract class Util {
    private Util() {

    }

    public static <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> futures) {
        CompletableFuture<List<T>> identity = CompletableFuture.completedFuture(new ArrayList<>(futures.size()));

        BiFunction<CompletableFuture<List<T>>, CompletableFuture<T>, CompletableFuture<List<T>>> combineToList = (acc, next) -> acc.thenCombine(next, (a, b) -> {
            a.add(b);
            return a;
        });

        BinaryOperator<CompletableFuture<List<T>>> combineLists = (a, b) -> a.thenCombine(b, (l1, l2) -> {
            l1.addAll(l2);
            return l1;
        });

        return futures.stream().reduce(identity, combineToList, combineLists);
    }


    public static <T> CompletableFuture<T> timeoutAfter(Duration duration, ScheduledExecutorService executorService) {
        CompletableFuture<T> promise = new CompletableFuture<>();
        executorService.schedule(() -> promise.completeExceptionally(new TimeoutException()), duration.toMillis(), TimeUnit.MILLISECONDS);
        return promise;
    }

    public static <T> CompletableFuture<T> race(CompletableFuture<T> a, CompletableFuture<T> b) {
        final CompletableFuture<T> promise = new CompletableFuture<>();
        final Lock lock = new ReentrantLock();
        completeSynchronized(lock, promise, a, b);
        completeSynchronized(lock, promise, b, a);

        return promise;
    }

    private static <T> void completeSynchronized(Lock lock, CompletableFuture<T> promise, CompletableFuture<T> first, CompletableFuture<T> second) {
        first.whenComplete((v, t) -> {
            lock.lock();
            try {
                if (!promise.isDone()) {
                    second.cancel(false);
                    if (t != null) {
                        promise.completeExceptionally(t);
                    } else {
                        promise.complete(v);
                    }
                }
            } finally {
                lock.unlock();
            }
        });
    }


    public static <T> Stream<T> enumerationAsStream(Enumeration<T> e) {
        return StreamSupport.stream(
                new Spliterators.AbstractSpliterator<T>(Long.MAX_VALUE, Spliterator.ORDERED) {
                    public boolean tryAdvance(Consumer<? super T> action) {
                        if (e.hasMoreElements()) {
                            action.accept(e.nextElement());
                            return true;
                        }
                        return false;
                    }

                    public void forEachRemaining(Consumer<? super T> action) {
                        while (e.hasMoreElements()) action.accept(e.nextElement());
                    }
                }, false);
    }

}
