plugins {
    id("org.cubeengine.parent.module")
}

val nuvotifierVersion: String by project.properties

dependencies {
    compileOnly("com.vexsoftware:nuvotifier-sponge8:${nuvotifierVersion}")
    compileOnly("com.vexsoftware:nuvotifier-common:${nuvotifierVersion}")
    compileOnly("com.vexsoftware:nuvotifier-api:${nuvotifierVersion}")
}
