plugins {
  id("net.neoforged.gradle.userdev") version "7.0.145"
}

dependencies {
  implementation("net.neoforged:neoforge:21.1.203")
}

configurations.configureEach {
  if (state != org.gradle.api.artifacts.Configuration.State.UNRESOLVED) {
    return@configureEach
  }

  resolutionStrategy.eachDependency {
    if (requested.group == "org.ow2.asm") {
      useVersion("9.8")
      because("Align ASM version with NeoForge userdev module-path jars")
    }
  }
}

// Expande ${version} en mods.toml
tasks.processResources {
  inputs.property("version", project.version)
  filesMatching("META-INF/mods.toml") {
    expand(mapOf("version" to project.version))
  }
}
