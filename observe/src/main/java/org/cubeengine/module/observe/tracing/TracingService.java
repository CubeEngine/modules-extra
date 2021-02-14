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

import io.opentracing.Span;
import io.opentracing.Tracer;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface TracingService {
    Tracer getTracer();

    default ExecutorService instrument(String label, ExecutorService service) {
        return new InstrumentedExecutorService(this, service, label);
    }

    default Runnable instrument(String label, Runnable f) {
        final Tracer tracer = getTracer();
        final Span span = getTracer().buildSpan(label).start();
        return () -> {
            tracer.activateSpan(span);
            try {
                f.run();
            } finally {
                span.finish();
            }
        };
    }

    default <V> Callable<V> instrument(String label, Callable<V> f) {
        final Tracer tracer = getTracer();
        final Span span = getTracer().buildSpan(label).start();
        return () -> {
            tracer.activateSpan(span);
            try {
                return f.call();
            } finally {
                span.finish();
            }
        };
    }

    default <T, R> Function<T, R> instrument(String label, Function<T, R> f) {
        final Tracer tracer = getTracer();
        final Span span = getTracer().buildSpan(label).start();
        return (v) -> {
            tracer.activateSpan(span);
            try {
                return f.apply(v);
            } finally {
                span.finish();
            }
        };
    }

    default <T, U, R> BiFunction<T, U, R> instrument(String label, BiFunction<T, U, R> f) {
        final Tracer tracer = getTracer();
        final Span span = getTracer().buildSpan(label).start();
        return (v, w) -> {
            tracer.activateSpan(span);
            try {
                return f.apply(v, w);
            } finally {
                span.finish();
            }
        };
    }

}
