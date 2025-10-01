package com.tuempresa.rogue.data.model;

import com.google.gson.JsonObject;
import com.tuempresa.rogue.data.DungeonDataException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DungeonDef {
    private final ResourceLocation id;
    private final int levelMin;
    private final int entryCost;
    private final ResourceLocation world;
    private final List<RoomDef> rooms;
    private final RewardsDef rewards;

    public DungeonDef(ResourceLocation id, int levelMin, int entryCost, ResourceLocation world, List<RoomDef> rooms, RewardsDef rewards) {
        this.id = id;
        this.levelMin = levelMin;
        this.entryCost = entryCost;
        this.world = world;
        this.rooms = Collections.unmodifiableList(new ArrayList<>(rooms));
        this.rewards = rewards;
    }

    public ResourceLocation id() {
        return id;
    }

    public int levelMin() {
        return levelMin;
    }

    public int entryCost() {
        return entryCost;
    }

    public ResourceLocation world() {
        return world;
    }

    public List<RoomDef> rooms() {
        return rooms;
    }

    public RewardsDef rewards() {
        return rewards;
    }

    public static DungeonDef fromJson(ResourceLocation fileId, JsonObject json) {
        ResourceLocation id = parseResourceId(json, "id", fileId);
        int levelMin = Math.max(1, GsonHelper.getAsInt(json, "levelMin", 1));
        int entryCost = Math.max(0, GsonHelper.getAsInt(json, "entryCost", 0));
        ResourceLocation world = parseResourceId(json, "world", fileId);

        if (!json.has("rooms")) {
            throw new DungeonDataException("La mazmorra " + id + " requiere rooms");
        }
        var roomsArray = GsonHelper.getAsJsonArray(json, "rooms");
        if (roomsArray.isEmpty()) {
            throw new DungeonDataException("La mazmorra " + id + " debe tener al menos una sala");
        }

        List<RoomDef> rooms = new ArrayList<>();
        roomsArray.forEach(element -> rooms.add(RoomDef.fromJson(GsonHelper.convertToJsonObject(element, "room"))));

        RewardsDef rewards = RewardsDef.fromJson(GsonHelper.getAsJsonObject(json, "rewards"));

        return new DungeonDef(id, levelMin, entryCost, world, rooms, rewards);
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
