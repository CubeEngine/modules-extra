package org.cubeengine.module.observe.health;

import java.util.concurrent.CompletableFuture;

public interface AsyncHealthProbe {
    CompletableFuture<HealthState> probe();
}
