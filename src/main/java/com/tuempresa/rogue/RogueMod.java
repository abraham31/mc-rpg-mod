package com.tuempresa.rogue;

import com.mojang.logging.LogUtils;
import com.tuempresa.rogue.config.RogueConfig;
import com.tuempresa.rogue.data.DungeonDataReloader;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModLoadingContext;
import org.slf4j.Logger;

/**
 * Entry point for the Rogue mod. Responsible for bootstrapping shared services
 * and exposing the central logger used throughout the project.
 */
@Mod(RogueMod.MOD_ID)
public final class RogueMod {
    public static final String MOD_ID = "rogue";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DungeonDataReloader DUNGEON_DATA = DungeonDataReloader.getInstance();

    public RogueMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RogueConfig.COMMON_SPEC, "rogue-common.toml");
        LOGGER.info("Inicializando el mod {}", MOD_ID);
    }

    /**
     * Crea un {@link ResourceLocation} dentro del espacio de nombres del mod.
     *
     * @param path el identificador relativo dentro del mod
     * @return la {@link ResourceLocation} resultante
     */
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
