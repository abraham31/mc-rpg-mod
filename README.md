# mc-rpg-mod

This repository contains the RPG core and content modules for the NeoForge-based Minecraft mod.

## Estado de los servicios

El proyecto está dividido en dos servicios principales (módulos Gradle). En esta iteración ambos
ya contienen la base para probar desplazamientos entre mapas personalizados:

| Servicio / Módulo  | Estado actual | Contenido relevante |
|--------------------|---------------|---------------------|
| `rpg-core`         | 🛠️ En preparación | Expone el comando `/rpg tp` para saltar entre dimensiones personalizadas, fija bordes del mundo por dimensión, aplica filtros simples de generación de criaturas y ofrece API pública para warps/tiendas/misiones. |
| `rpg-content-prontera` | ✅ Pack inicial | Aporta la ciudad base, Field 1, Field 2 y textos de localización bajo el mod id `rpg_content_prontera`. |

Ambos servicios comparten la misma configuración de build y se pueden compilar sin errores cuando
las dependencias de NeoForge están disponibles. A medida que se agreguen nuevas funciones, este
README se irá ampliando con instrucciones adicionales.

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

## Probar las dimensiones personalizadas

### Teletransporte de desarrollo

Durante el desarrollo puedes desplazarte rápidamente entre las dimensiones con el comando:

```mcfunction
/rpg tp <city|field1|field2>
```

Cada destino te teletransporta al punto de aparición compartido del nivel correspondiente. Si la
dimensión todavía no está cargada, el servidor la inicializa automáticamente antes del salto.

### Bordes del mundo

Para mantener mapas de tamaño controlado, cada dimensión registra un `WorldBorder` con radios
predefinidos:

| Dimensión                    | Radio aproximado |
|------------------------------|------------------|
| `rpg_content_prontera:city`      | 120 bloques (~240×240) |
| `rpg_content_prontera:field1`    | 256 bloques (~512×512) |
| `rpg_content_prontera:field2`    | 256 bloques (~512×512) |

Estos valores se pueden ajustar editando la clase `WorldEvents` en `rpg-core`.

### Spawns controlados

Las dimensiones de campo emplean un filtro sencillo de aparición de mobs para favorecer pruebas
tempranas:

* **Field 1:** permite *slimes* y conejos.
* **Field 2:** permite lobos y arañas.

El resto de criaturas se cancelan en el evento `MobSpawnEvent.FinalizeSpawn`. Esta aproximación se
sustituirá por *biome modifiers* a medida que se defina la fauna final de cada mapa.

## Compatibilidad de packs

| RPG Core | Prontera Pack | Armas Pack 1 | Mobs Pack 1 |
|----------|---------------|--------------|-------------|
| 1.0.x    | 1.0.x         | *(skeleton)* | *(skeleton)* |
