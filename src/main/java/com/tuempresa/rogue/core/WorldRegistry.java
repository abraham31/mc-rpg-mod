package com.tuempresa.rogue.core;

import com.tuempresa.rogue.util.RogueLogger;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

/**
 * Registro estático de dimensiones utilizadas por el mod.
 */
public final class WorldRegistry {
    public static final ResourceKey<LevelStem> CITY1_STEM =
        ResourceKey.create(Registries.LEVEL_STEM, RogueConstants.id("city1"));
    public static final ResourceKey<LevelStem> EARTH_STEM =
        ResourceKey.create(Registries.LEVEL_STEM, RogueConstants.id("earth_dim"));
    public static final ResourceKey<DimensionType> CITY1_DIMENSION_TYPE =
        ResourceKey.create(Registries.DIMENSION_TYPE, RogueConstants.id("city1"));
    public static final ResourceKey<DimensionType> EARTH_DIMENSION_TYPE =
        ResourceKey.create(Registries.DIMENSION_TYPE, RogueConstants.id("earth_dim"));

    private WorldRegistry() {
    }

    public static void registerDimensions() {
        RogueLogger.info("Registrando dimensiones del mod Rogue");
        createCity1();
        createEarthDim();
    }

    public static ResourceKey<Level> createCity1() {
        return RogueConstants.DIM_CITY1;
    }

    public static ResourceKey<Level> createEarthDim() {
        return RogueConstants.DIM_EARTH;
    }
}
