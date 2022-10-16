rootProject.name = "extra-aggregator"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.cubeengine.org")
        maven("https://repo.spongepowered.org/repository/maven-public")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        mavenLocal()
    }

    val conventionPluginVersion: String by settings
    plugins {
        id("org.cubeengine.parent.module") version (conventionPluginVersion)
    }
}

include("bigdata",
        "chat",
        "chopchop",
        "discord",
        "elevator",
        "fly",
        "headvillager",
        "itemduct",
        "kits",
        "mechanism",
        "namehistory",
        "observe",
        "powertools",
        "spawn",
        "spawner",
        "squelch",
        "tablist",
        "terra",
        "traders",
        "vigil",
        "vote",
        "writer"
)
