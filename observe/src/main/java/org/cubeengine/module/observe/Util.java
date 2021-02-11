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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;

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
}
