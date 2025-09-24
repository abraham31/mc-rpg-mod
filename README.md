# mc-rpg-mod

This repository contains the RPG core and content modules for the NeoForge-based Minecraft mod.

## Estado de los servicios

El proyecto está dividido en dos servicios principales (módulos Gradle) que todavía se
encuentran en fase de andamiaje. A continuación se documenta su estado actual:

| Servicio / Módulo     | Estado actual | Notas |
|-----------------------|---------------|-------|
| `rpg-core`            | 🛠️ En preparación | Incluye únicamente la estructura del paquete del mod (`pack.mcmeta`). Todavía no expone lógica de juego ni registradores NeoForge. |
| `rpg-content-base`    | 🛠️ En preparación | Contiene solo los metadatos mínimos del paquete (`pack.mcmeta`) para futuros assets y datos de contenido. |

Ambos servicios comparten la misma configuración de build y se pueden compilar sin errores, pero
ninguno publica APIs ni contenido todavía. Este README se actualizará conforme se implementen
funcionalidades jugables o datos adicionales.

## Build requirements

* Java 21 toolchain (configured via Gradle toolchains)
* Gradle 8.x installation (or use the Gradle wrapper if present)
* Internet access to `https://maven.neoforged.net/releases`

## Dependency pinning

The project now targets the tested NeoForge release `21.1.203`. If you previously built against a
`21.1.+` snapshot, refresh your dependencies to ensure Gradle fetches the pinned build:

```bash
gradle --refresh-dependencies --console=plain
```

After refreshing, run the usual build to verify the dependencies resolve correctly:

```bash
gradle :rpg-core:build --console=plain
```

If the NeoForge repositories are temporarily unavailable, Gradle may report HTTP errors while
resolving the `net.neoforged.gradle.userdev` plugin. Retry the build once access is restored.
