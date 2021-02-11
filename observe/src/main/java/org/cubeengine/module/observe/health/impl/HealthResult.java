package org.cubeengine.module.observe.health.impl;

import org.cubeengine.module.observe.health.HealthState;

import java.util.Map;

public class HealthResult {
    private final HealthState state;
    private final Map<String, HealthState> details;

    public HealthResult(HealthState state, Map<String, HealthState> details) {
        this.state = state;
        this.details = details;
    }

    public HealthState getState() {
        return state;
    }

    public Map<String, HealthState> getDetails() {
        return details;
    }
}
