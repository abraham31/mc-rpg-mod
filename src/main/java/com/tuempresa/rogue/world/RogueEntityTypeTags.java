package com.tuempresa.rogue.world;

import com.tuempresa.rogue.RogueMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/**
 * Colección de llaves de tags utilizadas por el mod. Mantenerlas centralizadas
 * evita duplicar cadenas mágicas y reduce errores tipográficos.
 */
public final class RogueEntityTypeTags {
    /** Tag de tipos de entidad permitidos para spawns naturales en las mazmorras. */
    public static final TagKey<EntityType<?>> ROGUE_MOBS =
        TagKey.create(Registries.ENTITY_TYPE, RogueMod.id("rogue_mob"));

    private RogueEntityTypeTags() {
    }
}
