plugins {
    id("org.cubeengine.parent.module")
}

val prometheusVersion: String by project.properties;

dependencies {

    implementation("org.spongepowered:observer:1.0-SNAPSHOT")
    // Exposition HTTPServer
    implementation("io.netty:netty-codec-http:4.1.82.Final")
    // Monitoring
    implementation("io.prometheus:simpleclient:$prometheusVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    // Tracing
    implementation("io.opentracing:opentracing-api:0.33.0")
    implementation("io.jaegertracing:jaeger-client:1.8.1")
}
