rootProject.name = "extra-aggregator"

pluginManagement {
    includeBuild("conventions")
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.spongepowered.org/repository/maven-public")
        mavenLocal()
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
