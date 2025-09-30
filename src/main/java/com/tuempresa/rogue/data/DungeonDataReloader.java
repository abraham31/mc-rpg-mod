package com.tuempresa.rogue.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tuempresa.rogue.RogueMod;
import com.tuempresa.rogue.data.model.DungeonDef;
import com.tuempresa.rogue.data.model.PortalDef;
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

/**
 * Reload listener responsible for parsing dungeon definitions.
 */
public final class DungeonDataReloader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DungeonDataReloader INSTANCE = new DungeonDataReloader();

    private Map<ResourceLocation, PortalDef> portalDefs = Map.of();
    private Map<ResourceLocation, DungeonDef> dungeonDefs = Map.of();

    private DungeonDataReloader() {
        super(GSON, "dungeons");
    }

    public static DungeonDataReloader getInstance() {
        return INSTANCE;
    }

    public Map<ResourceLocation, PortalDef> portals() {
        return portalDefs;
    }

    public Map<ResourceLocation, DungeonDef> dungeons() {
        return dungeonDefs;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> preparedData,
                         ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        Map<ResourceLocation, PortalDef> portals = new HashMap<>();
        Map<ResourceLocation, DungeonDef> dungeons = new HashMap<>();
        List<String> errors = new ArrayList<>();

        preparedData.forEach((fileId, jsonElement) -> {
            if (!RogueMod.MOD_ID.equals(fileId.getNamespace())) {
                return;
            }

            try {
                JsonObject root = GsonHelper.convertToJsonObject(jsonElement, "root");
                if (!root.has("portal")) {
                    throw new DungeonDataException("Falta la sección 'portal'");
                }
                if (!root.has("dungeon")) {
                    throw new DungeonDataException("Falta la sección 'dungeon'");
                }

                PortalDef portal = PortalDef.fromJson(fileId, GsonHelper.getAsJsonObject(root, "portal"));
                DungeonDef dungeon = DungeonDef.fromJson(fileId, GsonHelper.getAsJsonObject(root, "dungeon"));

                if (portals.putIfAbsent(portal.id(), portal) != null) {
                    throw new DungeonDataException("Portal duplicado con id " + portal.id());
                }
                if (dungeons.putIfAbsent(dungeon.id(), dungeon) != null) {
                    throw new DungeonDataException("Mazmorra duplicada con id " + dungeon.id());
                }
            } catch (DungeonDataException e) {
                errors.add("[" + fileId + "] " + e.getMessage());
            } catch (Exception e) {
                errors.add("[" + fileId + "] Error inesperado: " + e.getMessage());
            }
        });

        portals.values().forEach(portal -> {
            if (!dungeons.containsKey(portal.dungeonId())) {
                errors.add("El portal " + portal.id() + " referencia una mazmorra inexistente " + portal.dungeonId());
            }
        });

        if (!errors.isEmpty()) {
            errors.forEach(error -> RogueMod.LOGGER.error("Fallo cargando mazmorra: {}", error));
            throw new DungeonDataException("Se encontraron " + errors.size() + " errores al cargar las mazmorras");
        }

        this.portalDefs = Collections.unmodifiableMap(new HashMap<>(portals));
        this.dungeonDefs = Collections.unmodifiableMap(new HashMap<>(dungeons));
        RogueMod.LOGGER.info("Cargadas {} mazmorras y {} portales", dungeonDefs.size(), portalDefs.size());
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
