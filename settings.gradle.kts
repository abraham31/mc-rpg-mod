pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.neoforged.net/releases")
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://maven.neoforged.net/releases")
    }
}

rootProject.name = "rogue"
