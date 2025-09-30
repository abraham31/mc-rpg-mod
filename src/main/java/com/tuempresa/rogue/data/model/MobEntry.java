package com.tuempresa.rogue.data.model;

import com.google.gson.JsonObject;
import com.tuempresa.rogue.data.DungeonDataException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

/**
 * Entry describing a mob that will appear in a wave.
 */
public final class MobEntry {
    private final ResourceLocation entityType;
    private final int count;
    private final int spawnDelay;

    public MobEntry(ResourceLocation entityType, int count, int spawnDelay) {
        this.entityType = entityType;
        this.count = count;
        this.spawnDelay = spawnDelay;
    }

    public ResourceLocation entityType() {
        return entityType;
    }

    public int count() {
        return count;
    }

    public int spawnDelay() {
        return spawnDelay;
    }

    public static MobEntry fromJson(JsonObject json) {
        String rawType = GsonHelper.getAsString(json, "type");
        ResourceLocation entityType = ResourceLocation.tryParse(rawType);
        if (entityType == null) {
            throw new DungeonDataException("Tipo de entidad inválido: " + rawType);
        }

        int count = GsonHelper.getAsInt(json, "count", 1);
        if (count <= 0) {
            throw new DungeonDataException("El valor count debe ser positivo para el mob " + entityType);
        }

        int spawnDelay = GsonHelper.getAsInt(json, "spawn_delay", 0);
        if (spawnDelay < 0) {
            throw new DungeonDataException("El spawn_delay no puede ser negativo para el mob " + entityType);
        }

        return new MobEntry(entityType, count, spawnDelay);
    }
}
