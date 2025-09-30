package com.tuempresa.rogue.data.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tuempresa.rogue.data.DungeonDataException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Rewards granted after completing a dungeon.
 */
public final class RewardsDef {
    private final int experience;
    private final List<ResourceLocation> lootTables;

    public RewardsDef(int experience, List<ResourceLocation> lootTables) {
        this.experience = experience;
        this.lootTables = Collections.unmodifiableList(new ArrayList<>(lootTables));
    }

    public int experience() {
        return experience;
    }

    public List<ResourceLocation> lootTables() {
        return lootTables;
    }

    public static RewardsDef fromJson(JsonObject json) {
        int experience = GsonHelper.getAsInt(json, "experience", 0);
        if (experience < 0) {
            throw new DungeonDataException("El valor de experience no puede ser negativo");
        }

        List<ResourceLocation> lootTables = new ArrayList<>();
        if (json.has("loot_tables")) {
            JsonArray array = GsonHelper.getAsJsonArray(json, "loot_tables");
            for (JsonElement element : array) {
                String rawId = element.getAsString();
                ResourceLocation id = ResourceLocation.tryParse(rawId);
                if (id == null) {
                    throw new DungeonDataException("Loot table inválida: " + rawId);
                }
                lootTables.add(id);
            }
        }

        return new RewardsDef(experience, lootTables);
    }
}
