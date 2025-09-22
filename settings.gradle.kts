pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://maven.neoforged.net/releases")
  }
}
rootProject.name = "rpg-mmo"
include(":rpg-core", ":rpg-content-base")
