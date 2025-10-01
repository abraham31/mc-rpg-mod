package com.tuempresa.rogue.data.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tuempresa.rogue.data.DungeonDataException;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WaveDef {
    private final int cooldownTicks;
    private final int maxAlive;
    private final List<MobEntry> packs;

    public WaveDef(int cooldownTicks, int maxAlive, List<MobEntry> packs) {
        this.cooldownTicks = cooldownTicks;
        this.maxAlive = maxAlive;
        this.packs = Collections.unmodifiableList(new ArrayList<>(packs));
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public int maxAlive() {
        return maxAlive;
    }

    public List<MobEntry> packs() {
        return packs;
    }

    public static WaveDef fromJson(JsonObject json) {
        int cooldown = Math.max(0, GsonHelper.getAsInt(json, "cooldownTicks", 0));
        int maxAlive = Math.max(1, GsonHelper.getAsInt(json, "maxAlive", 1));

        if (!json.has("packs")) {
            throw new DungeonDataException("La wave requiere packs de mobs");
        }

        JsonArray packsArray = GsonHelper.getAsJsonArray(json, "packs");
        if (packsArray.isEmpty()) {
            throw new DungeonDataException("Los packs de la wave no pueden estar vacíos");
        }

        List<MobEntry> packs = new ArrayList<>();
        for (JsonElement element : packsArray) {
            JsonObject packObject = GsonHelper.convertToJsonObject(element, "pack");
            packs.add(MobEntry.fromJson(packObject));
        }

        return new WaveDef(cooldown, maxAlive, packs);
    }
}
