rootProject.name = "PanoramaPolihnitou"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        // RootEncoder (Android RTMP live streaming) is only published on JitPack;
        // scoped so nothing else can resolve from there.
        maven("https://jitpack.io") {
            mavenContent { includeGroupAndSubgroups("com.github.pedroSG94") }
        }
    }
}

include(":composeApp")
