package com.tuempresa.rogue.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tuempresa.rogue.RogueMod;
import com.tuempresa.rogue.core.RogueConstants;
import com.tuempresa.rogue.data.model.ItemConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Reload listener responsible for parsing generic item configuration files.
 */
public final class ItemConfigReloader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ItemConfigReloader INSTANCE = new ItemConfigReloader();

    private Map<ResourceLocation, ItemConfig> itemConfigs = Map.of();

    private ItemConfigReloader() {
        super(GSON, "config/items");
    }

    public static ItemConfigReloader getInstance() {
        return INSTANCE;
    }

    public Optional<ItemConfig> get(ResourceLocation itemId) {
        return Optional.ofNullable(itemConfigs.get(itemId));
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> preparedData,
                         ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        Map<ResourceLocation, ItemConfig> configs = new HashMap<>();

        preparedData.forEach((fileId, jsonElement) -> {
            if (!RogueConstants.MOD_ID.equals(fileId.getNamespace())) {
                return;
            }

            try {
                JsonObject root = GsonHelper.convertToJsonObject(jsonElement, "root");
                ItemConfig config = ItemConfig.fromJson(fileId, root);
                configs.put(fileId, config);
            } catch (ItemConfigException e) {
                RogueMod.LOGGER.error("Fallo cargando configuración de ítem {}: {}", fileId, e.getMessage());
            } catch (Exception e) {
                RogueMod.LOGGER.error("Error inesperado cargando configuración de ítem {}: {}", fileId, e.getMessage());
            }
        });

        itemConfigs = Collections.unmodifiableMap(configs);
        RogueMod.LOGGER.info("Cargadas {} configuraciones de ítems", itemConfigs.size());
    }

    public void reloadNow(ResourceManager resourceManager) {
        try {
            Map<ResourceLocation, JsonElement> prepared = this.prepare(resourceManager, ProfilerFiller.EMPTY);
            this.apply(prepared, resourceManager, ProfilerFiller.EMPTY);
        } catch (Exception e) {
            throw new ItemConfigException("Error recargando configuraciones de ítems", e);
        }
    }

    public void reloadNow(MinecraftServer server) {
        reloadNow(server.getServerResources().resourceManager());
    }
}
