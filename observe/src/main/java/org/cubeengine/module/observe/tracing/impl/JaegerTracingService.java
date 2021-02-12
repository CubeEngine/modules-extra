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
package org.cubeengine.module.observe.tracing.impl;

import io.jaegertracing.Configuration;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.spi.MetricsFactory;
import io.opentracing.Tracer;
import org.cubeengine.module.observe.tracing.TracingService;

public class JaegerTracingService implements TracingService {

    private final JaegerTracer tracer;

    public JaegerTracingService(String serviceName, MetricsFactory metricsFactory) {
        this.tracer = Configuration
                .fromEnv(serviceName)
                .withMetricsFactory(metricsFactory)
                .getTracer();
    }

    @Override
    public Tracer getTracer() {
        return tracer;
    }
}
