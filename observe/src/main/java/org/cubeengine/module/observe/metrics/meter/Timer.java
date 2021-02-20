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
