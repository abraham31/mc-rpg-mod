package com.tuempresa.rogue.data.model;

import com.google.gson.JsonObject;
import com.tuempresa.rogue.data.DungeonDataException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.Objects;

/**
 * Definition for a portal that allows players to access a dungeon.
 */
public final class PortalDef {
    private final ResourceLocation id;
    private final ResourceLocation dungeonId;
    private final String displayName;
    private final String description;
    private final int requiredLevel;
    private final int activationCost;

    public PortalDef(ResourceLocation id,
                     ResourceLocation dungeonId,
                     String displayName,
                     String description,
                     int requiredLevel,
                     int activationCost) {
        this.id = Objects.requireNonNull(id, "id");
        this.dungeonId = Objects.requireNonNull(dungeonId, "dungeonId");
        this.displayName = displayName;
        this.description = description;
        this.requiredLevel = requiredLevel;
        this.activationCost = activationCost;
    }

    public ResourceLocation id() {
        return id;
    }

    public ResourceLocation dungeonId() {
        return dungeonId;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public int requiredLevel() {
        return requiredLevel;
    }

    public int activationCost() {
        return activationCost;
    }

    public static PortalDef fromJson(ResourceLocation fileId, JsonObject json) {
        ResourceLocation id = parseResource(json, "id", fileId);
        ResourceLocation dungeonId = parseResource(json, "dungeon");
        String displayName = normalizeText(GsonHelper.getAsString(json, "display_name", id.toString()));
        String description = normalizeText(GsonHelper.getAsString(json, "description", ""));
        int requiredLevel = GsonHelper.getAsInt(json, "required_level", 0);
        int activationCost = GsonHelper.getAsInt(json, "activation_cost", 0);

        if (requiredLevel < 0) {
            throw new DungeonDataException("El portal " + id + " tiene required_level negativo");
        }
        if (activationCost < 0) {
            throw new DungeonDataException("El portal " + id + " tiene activation_cost negativo");
        }
        if (displayName.isBlank()) {
            throw new DungeonDataException("El portal " + id + " requiere un display_name no vacío");
        }

        return new PortalDef(id, dungeonId, displayName, description, requiredLevel, activationCost);
    }

    private static ResourceLocation parseResource(JsonObject json, String field) {
        if (!json.has(field)) {
            throw new DungeonDataException("Falta el campo obligatorio '" + field + "'");
        }
        return parseResource(json, field, null);
    }

    private static ResourceLocation parseResource(JsonObject json, String field, ResourceLocation fallback) {
        String raw = GsonHelper.getAsString(json, field, fallback != null ? fallback.toString() : null);
        if (raw == null) {
            throw new DungeonDataException("Falta el campo obligatorio '" + field + "'");
        }
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) {
            throw new DungeonDataException("El valor '" + raw + "' del campo '" + field + "' no es un identificador válido");
        }
        return id;
    }

    private static String normalizeText(String text) {
        return text == null ? "" : text.trim();
    }
}
