package com.tuempresa.rogue.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tuempresa.rogue.core.RogueConstants;
import com.tuempresa.rogue.RogueMod;
import com.tuempresa.rogue.data.model.DungeonDef;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reload listener responsible for parsing dungeon definitions.
 */
public final class DungeonDataReloader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DungeonDataReloader INSTANCE = new DungeonDataReloader();

    private Map<ResourceLocation, DungeonDef> dungeonDefs = Map.of();

    private DungeonDataReloader() {
        super(GSON, "dungeons");
    }

    public static DungeonDataReloader getInstance() {
        return INSTANCE;
    }

    public Map<ResourceLocation, DungeonDef> dungeons() {
        return dungeonDefs;
    }

    public Optional<DungeonDef> getDungeon(String id) {
        ResourceLocation resourceId = ResourceLocation.tryParse(id);
        if (resourceId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(dungeonDefs.get(resourceId));
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> preparedData,
                         ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        Map<ResourceLocation, DungeonDef> dungeons = new HashMap<>();
        List<String> errors = new ArrayList<>();

        preparedData.forEach((fileId, jsonElement) -> {
            if (!RogueConstants.MOD_ID.equals(fileId.getNamespace())) {
                return;
            }

            try {
                JsonObject root = GsonHelper.convertToJsonObject(jsonElement, "root");
                DungeonDef dungeon = DungeonDef.fromJson(fileId, root);
                if (dungeons.putIfAbsent(dungeon.id(), dungeon) != null) {
                    throw new DungeonDataException("Mazmorra duplicada con id " + dungeon.id());
                }
            } catch (DungeonDataException e) {
                errors.add("[" + fileId + "] " + e.getMessage());
            } catch (Exception e) {
                errors.add("[" + fileId + "] Error inesperado: " + e.getMessage());
            }
        });

        if (!errors.isEmpty()) {
            errors.forEach(error -> RogueMod.LOGGER.error("Fallo cargando mazmorra: {}", error));
            throw new DungeonDataException("Se encontraron " + errors.size() + " errores al cargar las mazmorras");
        }

        this.dungeonDefs = Collections.unmodifiableMap(new HashMap<>(dungeons));
        RogueMod.LOGGER.info("Cargadas {} mazmorras", dungeonDefs.size());
    }

    public void reloadNow(ResourceManager resourceManager) {
        try {
            Map<ResourceLocation, JsonElement> prepared = this.prepare(resourceManager, ProfilerFiller.EMPTY);
            this.apply(prepared, resourceManager, ProfilerFiller.EMPTY);
        } catch (DungeonDataException e) {
            throw e;
        } catch (Exception e) {
            throw new DungeonDataException("Error recargando datos de mazmorras", e);
        }
    }

    public void reloadNow(MinecraftServer server) {
        reloadNow(server.getServerResources().resourceManager());
    }
}
