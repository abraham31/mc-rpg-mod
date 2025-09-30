package com.tuempresa.rogue.data.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tuempresa.rogue.data.DungeonDataException;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A wave within a dungeon room.
 */
public final class WaveDef {
    private final int index;
    private final int warmupTicks;
    private final List<MobEntry> mobs;

    public WaveDef(int index, int warmupTicks, List<MobEntry> mobs) {
        this.index = index;
        this.warmupTicks = warmupTicks;
        this.mobs = Collections.unmodifiableList(new ArrayList<>(mobs));
    }

    public int index() {
        return index;
    }

    public int warmupTicks() {
        return warmupTicks;
    }

    public List<MobEntry> mobs() {
        return mobs;
    }

    public static WaveDef fromJson(JsonObject json) {
        int index = GsonHelper.getAsInt(json, "index");
        if (index <= 0) {
            throw new DungeonDataException("Cada wave necesita un index positivo");
        }

        int warmupTicks = GsonHelper.getAsInt(json, "warmup_ticks", 0);
        if (warmupTicks < 0) {
            throw new DungeonDataException("El warmup_ticks no puede ser negativo para la wave " + index);
        }

        if (!json.has("mobs")) {
            throw new DungeonDataException("La wave " + index + " requiere una lista de mobs");
        }

        JsonArray mobsArray = GsonHelper.getAsJsonArray(json, "mobs");
        if (mobsArray.isEmpty()) {
            throw new DungeonDataException("La wave " + index + " debe definir al menos un mob");
        }

        List<MobEntry> mobs = new ArrayList<>();
        for (JsonElement element : mobsArray) {
            JsonObject mobObject = GsonHelper.convertToJsonObject(element, "mob");
            mobs.add(MobEntry.fromJson(mobObject));
        }

        return new WaveDef(index, warmupTicks, mobs);
    }
}
