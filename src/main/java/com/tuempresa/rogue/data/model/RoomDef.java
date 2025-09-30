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
 * Definition of a dungeon room composed of multiple waves.
 */
public final class RoomDef {
    private final String id;
    private final List<WaveDef> waves;

    public RoomDef(String id, List<WaveDef> waves) {
        this.id = id;
        this.waves = Collections.unmodifiableList(new ArrayList<>(waves));
    }

    public String id() {
        return id;
    }

    public List<WaveDef> waves() {
        return waves;
    }

    public static RoomDef fromJson(JsonObject json) {
        String id = GsonHelper.getAsString(json, "id").trim();
        if (id.isEmpty()) {
            throw new DungeonDataException("Cada sala requiere un id no vacío");
        }

        if (!json.has("waves")) {
            throw new DungeonDataException("La sala " + id + " debe definir waves");
        }

        JsonArray wavesArray = GsonHelper.getAsJsonArray(json, "waves");
        if (wavesArray.isEmpty()) {
            throw new DungeonDataException("La sala " + id + " debe tener al menos una wave");
        }

        List<WaveDef> waves = new ArrayList<>();
        for (JsonElement element : wavesArray) {
            JsonObject waveObj = GsonHelper.convertToJsonObject(element, "wave");
            waves.add(WaveDef.fromJson(waveObj));
        }

        return new RoomDef(id, waves);
    }
}
