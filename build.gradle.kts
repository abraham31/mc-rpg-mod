import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    idea
    `java-library`
    id("net.neoforged.moddev") version "0.1.121"
}

val modId = "rogue"

group = "com.tuempresa.rogue"
version = "0.1.0"

base {
    archivesName.set(modId)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases")
}

neoForge {
    version = "21.1.1"
    mappings {
        official()
    }
}

sourceSets {
    main {
        resources {
            srcDir("src/generated/resources")
        }
    }
}

dependencies {
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<ProcessResources>().configureEach {
    inputs.property("version", project.version)

    filesMatching("META-INF/mods.toml") {
        expand(
            mapOf(
                "version" to project.version
            )
        )
    }
}

tasks.jar {
    manifest {
        attributes(
            "Specification-Title" to modId,
            "Specification-Vendor" to "Tu Empresa",
            "Specification-Version" to "1",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "Tu Empresa"
        )
    }
}
