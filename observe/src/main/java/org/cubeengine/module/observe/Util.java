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
