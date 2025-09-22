// Solo recursos; sigue usando Java 21 por uniformidad
tasks.processResources {
  inputs.property("version", project.version)
  filesMatching("META-INF/neoforge.mods.toml") {
    expand(mapOf("version" to project.version))
  }
}
