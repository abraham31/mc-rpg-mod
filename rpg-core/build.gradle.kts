plugins {
  id("net.neoforged.gradle.userdev") version "7.0.145"
}

dependencies {
  implementation("net.neoforged:neoforge:21.1.203")
}

// Expande ${version} en mods.toml
tasks.processResources {
  inputs.property("version", project.version)
  filesMatching("META-INF/mods.toml") {
    expand(mapOf("version" to project.version))
  }
}
