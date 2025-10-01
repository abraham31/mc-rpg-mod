package com.tuempresa.rogue.data.model;

import com.google.gson.JsonObject;
import com.tuempresa.rogue.data.DungeonDataException;
import net.minecraft.util.GsonHelper;

import java.util.Collections;
import java.util.List;

public final class RoomDef {
    private final String id;
    private final int[] boundsMin;
    private final int[] boundsMax;
    private final WaveDef wave;
    private final List<MobEntry> mobs;
    private final String affinityTag;

    public RoomDef(String id, int[] boundsMin, int[] boundsMax, WaveDef wave, List<MobEntry> mobs, String affinityTag) {
        this.id = id;
        this.boundsMin = boundsMin.clone();
        this.boundsMax = boundsMax.clone();
        this.wave = wave;
        this.mobs = Collections.unmodifiableList(mobs);
        this.affinityTag = affinityTag;
    }

    public String id() {
        return id;
    }

    public int[] boundsMin() {
        return boundsMin;
    }

    public int[] boundsMax() {
        return boundsMax;
    }

    public WaveDef wave() {
        return wave;
    }

    public List<MobEntry> mobs() {
        return mobs;
    }

    public String affinityTag() {
        return affinityTag;
    }

    public static RoomDef fromJson(JsonObject json) {
        String id = GsonHelper.getAsString(json, "id").trim();
        if (id.isEmpty()) {
            throw new DungeonDataException("Cada sala requiere un id no vacío");
        }

        int[] min = parseVector(json.getAsJsonObject("bounds"), "min");
        int[] max = parseVector(json.getAsJsonObject("bounds"), "max");

        WaveDef wave = WaveDef.fromJson(GsonHelper.getAsJsonObject(json, "wave"));

        List<MobEntry> mobs;
        if (json.has("mobs")) {
            var array = json.getAsJsonArray("mobs");
            var list = new java.util.ArrayList<MobEntry>();
            array.forEach(element -> list.add(MobEntry.fromJson(GsonHelper.convertToJsonObject(element, "mob"))));
            mobs = Collections.unmodifiableList(list);
        } else {
            mobs = Collections.emptyList();
        }

        String affinityTag = GsonHelper.getAsString(json, "affinityTag", "");

        return new RoomDef(id, min, max, wave, mobs, affinityTag);
    }

    private static int[] parseVector(JsonObject bounds, String key) {
        if (bounds == null || !bounds.has(key)) {
            throw new DungeonDataException("Faltan coordenadas de bounds." + key);
        }
        var array = bounds.getAsJsonArray(key);
        if (array.size() != 3) {
            throw new DungeonDataException("El vector " + key + " debe contener 3 enteros");
        }
        int[] result = new int[3];
        for (int i = 0; i < 3; i++) {
            result[i] = array.get(i).getAsInt();
        }
        return result;
    }
}
