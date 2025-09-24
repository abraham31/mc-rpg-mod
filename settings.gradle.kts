pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://maven.neoforged.net/releases")
  }
}
rootProject.name = "rpg-mmo"
include(":rpg-core", ":rpg-content-prontera")
// include(":rpg-weapons-pack-1", ":rpg-mobs-pack-1")
