plugins {
    id("org.cubeengine.parent.module")
}

val prometheusVersion: String by project.properties;

dependencies {

    implementation("org.spongepowered:observer:1.0-SNAPSHOT") // TODO shadow
    // Exposition HTTPServer
    implementation("io.netty:netty-codec-http:4.1.71.Final") // TODO compile dep?
    // Monitoring
    implementation("io.prometheus:simpleclient:$prometheusVersion") // TODO shadow
    implementation("io.prometheus:simpleclient_common:$prometheusVersion") // TODO shadow
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion") // TODO shadow
    // Tracing
    implementation("io.opentracing:opentracing-api:0.33.0") // TODO shadow
    implementation("io.jaegertracing:jaeger-client:1.5.0") // TODO shadow
}