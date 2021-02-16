package org.cubeengine.module.observe.metrics.meter;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

public interface Timer {
    void observe(double seconds);

    default RunningTimer start() {
        final long startTime = System.nanoTime();
        return () -> observe(System.nanoTime() - startTime);
    }

    default void time(Runnable f) {
        final long startTime = System.nanoTime();
        try {
            f.run();
        } finally {
            observe(System.nanoTime() - startTime);
        }
    }

    default <T> T time(Supplier<T> f) {
        final long startTime = System.nanoTime();
        try {
            return f.get();
        } finally {
            observe(System.nanoTime() - startTime);
        }
    }

    default <T> T time(Callable<T> f) throws Exception {
        final long startTime = System.nanoTime();
        try {
            return f.call();
        } finally {
            observe(System.nanoTime() - startTime);
        }
    }

    interface RunningTimer extends AutoCloseable {
        void stop();

        @Override
        default void close() throws Exception {
            stop();
        }
    }
}
