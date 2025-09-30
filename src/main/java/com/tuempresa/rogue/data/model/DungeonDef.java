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
 * Definition of a dungeon composed of rooms and rewards.
 */
public final class DungeonDef {
    private final ResourceLocation id;
    private final String displayName;
    private final List<RoomDef> rooms;
    private final RewardsDef rewards;

    public DungeonDef(ResourceLocation id, String displayName, List<RoomDef> rooms, RewardsDef rewards) {
        this.id = id;
        this.displayName = displayName;
        this.rooms = Collections.unmodifiableList(new ArrayList<>(rooms));
        this.rewards = rewards;
    }

    public ResourceLocation id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public List<RoomDef> rooms() {
        return rooms;
    }

    public RewardsDef rewards() {
        return rewards;
    }

    public static DungeonDef fromJson(ResourceLocation fileId, JsonObject json) {
        ResourceLocation id = parseResourceId(json, "id", fileId);
        String displayName = GsonHelper.getAsString(json, "display_name", id.toString()).trim();
        if (displayName.isEmpty()) {
            throw new DungeonDataException("La mazmorra " + id + " requiere display_name");
        }

        if (!json.has("rooms")) {
            throw new DungeonDataException("La mazmorra " + id + " requiere rooms");
        }
        JsonArray roomsArray = GsonHelper.getAsJsonArray(json, "rooms");
        if (roomsArray.isEmpty()) {
            throw new DungeonDataException("La mazmorra " + id + " debe tener al menos una sala");
        }

        List<RoomDef> rooms = new ArrayList<>();
        for (JsonElement element : roomsArray) {
            JsonObject roomObject = GsonHelper.convertToJsonObject(element, "room");
            rooms.add(RoomDef.fromJson(roomObject));
        }

        if (!json.has("rewards")) {
            throw new DungeonDataException("La mazmorra " + id + " requiere rewards");
        }
        RewardsDef rewards = RewardsDef.fromJson(GsonHelper.getAsJsonObject(json, "rewards"));

        return new DungeonDef(id, displayName, rooms, rewards);
    }

    private static ResourceLocation parseResourceId(JsonObject json, String field, ResourceLocation fallback) {
        String rawId = GsonHelper.getAsString(json, field, fallback.toString());
        ResourceLocation id = ResourceLocation.tryParse(rawId);
        if (id == null) {
            throw new DungeonDataException("Identificador inválido para la mazmorra: " + rawId);
        }
        return id;
    }
}
