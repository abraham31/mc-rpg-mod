# Rogue NeoForge Mod Skeleton

Este repositorio contiene la estructura mínima necesaria para comenzar a
construir el mod **Rogue** sobre NeoForge 1.21.1.

## Requisitos

- Java 21
- Gradle 8.8 o superior

## Estructura

- `build.gradle.kts`: configuración del proyecto y de NeoForge.
- `src/main/java/com/tuempresa/rogue`: código Java fuente del mod.
- `src/main/resources`: recursos y archivo `mods.toml`.
- `src/generated/resources`: ruta reservada para datos generados.

## Tareas de Gradle útiles

- `gradle runClient`: inicia Minecraft en modo cliente con el mod cargado.
- `gradle runServer`: inicia el servidor dedicado.
- `gradle runData`: genera datos en `src/generated/resources`.

## Punto de entrada y logger

La clase `com.tuempresa.rogue.RogueMod` actúa como punto de entrada del mod,
registra bloques, dimensiones y eventos clave durante la inicialización y expone
el logger principal mediante la constante `LOGGER` para reutilizarlo en todo el
código.
