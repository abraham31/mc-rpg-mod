package com.tuempresa.rogue.core;

import com.tuempresa.rogue.config.RogueConfig;
import com.tuempresa.rogue.data.DungeonDataReloader;
import com.tuempresa.rogue.util.RogueLogger;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Punto de entrada principal del mod.
 */
@Mod(RogueConstants.MOD_ID)
public final class RogueMod {
    public static final String MOD_ID = RogueConstants.MOD_ID;
    public static final org.slf4j.Logger LOGGER = RogueLogger.raw();
    public static final DungeonDataReloader DUNGEON_DATA = DungeonDataReloader.getInstance();

    public RogueMod() {
        init();
    }

    private void init() {
        RogueLogger.info("Inicializando Rogue Mod");
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, RogueConfig.COMMON_SPEC, "rogue-common.toml");
        WorldRegistry.registerDimensions();
        registerEvents();
        registerCommands();
    }

    public static net.minecraft.resources.ResourceLocation id(String path) {
        return RogueConstants.id(path);
    }

    private void registerEvents() {
        NeoForge.EVENT_BUS.register(new RogueServerEvents());
    }

    private void registerCommands() {
        NeoForge.EVENT_BUS.register(new RogueCommandEvents());
    }
}
