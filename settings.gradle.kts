enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    @Suppress("UnstableApiUsage")
    includeBuild("nucleus-build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.xpdustry.fr/snapshots/") {
            name = "xpdustry-snapshots"
            mavenContent { snapshotsOnly() }
        }
    }
}

rootProject.name = "nucleus-parent"

include("nucleus-api")
include("nucleus-common")
include("nucleus-discord")
include("nucleus-mindustry")
include("nucleus-testing")
