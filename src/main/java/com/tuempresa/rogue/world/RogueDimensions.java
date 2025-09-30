package com.tuempresa.rogue.world;

import com.tuempresa.rogue.RogueMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

/**
 * Static references to the custom dimensions used by the mod. The actual
 * layout of these dimensions is defined through JSON data packs, but having
 * strongly typed keys on the Java side simplifies teleportation and world
 * management code.
 */
public final class RogueDimensions {
    /** Hub dimension identifier: {@code rogue:city1}. */
    public static final ResourceKey<Level> CITY1_LEVEL =
        ResourceKey.create(Registries.DIMENSION, RogueMod.id("city1"));

    /** Dungeon dimension identifier: {@code rogue:earth_dim}. */
    public static final ResourceKey<Level> EARTH_DUNGEON_LEVEL =
        ResourceKey.create(Registries.DIMENSION, RogueMod.id("earth_dim"));

    /** Level stem key for the hub dimension, useful during world creation. */
    public static final ResourceKey<LevelStem> CITY1_LEVEL_STEM =
        ResourceKey.create(Registries.LEVEL_STEM, RogueMod.id("city1"));

    /** Level stem key for the dungeon dimension. */
    public static final ResourceKey<LevelStem> EARTH_DUNGEON_LEVEL_STEM =
        ResourceKey.create(Registries.LEVEL_STEM, RogueMod.id("earth_dim"));

    /** Dimension type key for the hub dimension. */
    public static final ResourceKey<DimensionType> HUB_DIMENSION_TYPE =
        ResourceKey.create(Registries.DIMENSION_TYPE, RogueMod.id("hub"));

    /** Dimension type key for the dungeon dimension. */
    public static final ResourceKey<DimensionType> DUNGEON_DIMENSION_TYPE =
        ResourceKey.create(Registries.DIMENSION_TYPE, RogueMod.id("dungeon"));

    private RogueDimensions() {
    }
}
