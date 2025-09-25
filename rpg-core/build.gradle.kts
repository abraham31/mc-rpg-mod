import net.neoforged.gradle.userdev.UserDevPluginExtension

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

extensions.configure<UserDevPluginExtension>("userdev") {
  runs {
    named("client") {
      mods {
        create("rpg_core") {
          source(sourceSets.main.get())
        }
        create("rpg_content_prontera") {
          source(project(":rpg-content-prontera").sourceSets.main.get())
        }
      }
    }
    named("server") {
      mods {
        create("rpg_core") {
          source(sourceSets.main.get())
        }
        create("rpg_content_prontera") {
          source(project(":rpg-content-prontera").sourceSets.main.get())
        }
      }
    }
  }
}

// Expande ${version} en mods.toml
tasks.processResources {
  inputs.property("version", project.version)
  filesMatching("META-INF/neoforge.mods.toml") {
    expand(mapOf("version" to project.version))
  }
}
