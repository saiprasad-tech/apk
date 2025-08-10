pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://dl.google.com/android/maven2")
        maven("https://maven.google.com")
    }
}

rootProject.name = "pixhawk-gcs-lite"
include(":app")