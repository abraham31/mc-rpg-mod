# mc-rpg-mod

This repository contains the RPG core and content modules for the NeoForge-based Minecraft mod.

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
