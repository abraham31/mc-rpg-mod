package com.tuempresa.rogue.data.model;

import com.google.gson.JsonObject;
import com.tuempresa.rogue.data.DungeonDataException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public final class MobEntry {
    private final ResourceLocation id;
    private final int weight;
    private final CompoundTag nbt;

    public MobEntry(ResourceLocation id, int weight, CompoundTag nbt) {
        this.id = id;
        this.weight = weight;
        this.nbt = nbt;
    }

    public ResourceLocation id() {
        return id;
    }

    public int weight() {
        return weight;
    }

    public CompoundTag nbt() {
        return nbt;
    }

    public static MobEntry fromJson(JsonObject json) {
        String rawId = GsonHelper.getAsString(json, "id");
        ResourceLocation id = ResourceLocation.tryParse(rawId);
        if (id == null) {
            throw new DungeonDataException("Identificador de mob inválido: " + rawId);
        }

        int weight = GsonHelper.getAsInt(json, "weight", 1);
        if (weight <= 0) {
            throw new DungeonDataException("El peso debe ser positivo para " + id);
        }

        CompoundTag tag = new CompoundTag();
        if (json.has("nbt")) {
            try {
                tag = TagParser.parseTag(GsonHelper.getAsString(json, "nbt"));
            } catch (Exception e) {
                throw new DungeonDataException("NBT inválido para " + id + ": " + e.getMessage());
            }
        }

        return new MobEntry(id, weight, tag);
    }
}
