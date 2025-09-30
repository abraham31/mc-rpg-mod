package com.tuempresa.rogue.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.border.WorldBorder;

/**
 * Utility helpers focused on configuring world borders. They provide the most
 * common operations required by the mod when preparing dungeon runs.
 */
public final class WorldBorderUtil {
    private static final double DEFAULT_DIAMETER = 5.9999968E7D;
    private static final double DEFAULT_DAMAGE_PER_BLOCK = 0.2D;
    private static final double DEFAULT_DAMAGE_BUFFER = 5.0D;
    private static final int DEFAULT_WARNING_BLOCKS = 5;
    private static final int DEFAULT_WARNING_TIME = 15;

    private WorldBorderUtil() {
    }

    /**
     * Configures a world border using a center position and diameter.
     *
     * @param level level that owns the world border
     * @param centerX X coordinate for the border center
     * @param centerZ Z coordinate for the border center
     * @param diameter size of the border in blocks
     */
    public static void configure(ServerLevel level, double centerX, double centerZ, double diameter) {
        configure(level, centerX, centerZ, diameter, DEFAULT_DAMAGE_PER_BLOCK, DEFAULT_DAMAGE_BUFFER);
    }

    /**
     * Configures a world border with extended parameters.
     */
    public static void configure(ServerLevel level, double centerX, double centerZ, double diameter,
                                 double damagePerBlock, double damageBuffer) {
        WorldBorder border = level.getWorldBorder();
        border.setCenter(centerX, centerZ);
        border.setSize(diameter);
        border.setDamagePerBlock(damagePerBlock);
        border.setDamageSafeZone(damageBuffer);
    }

    /**
     * Adjusts the warning configuration for a world border.
     */
    public static void setWarnings(ServerLevel level, int warningBlocks, int warningTimeSeconds) {
        WorldBorder border = level.getWorldBorder();
        border.setWarningBlocks(warningBlocks);
        border.setWarningTime(warningTimeSeconds);
    }

    /**
     * Restores vanilla defaults to the world border of the provided level.
     */
    public static void reset(ServerLevel level) {
        WorldBorder border = level.getWorldBorder();
        border.setCenter(0.0D, 0.0D);
        border.setSize(DEFAULT_DIAMETER);
        border.setDamagePerBlock(DEFAULT_DAMAGE_PER_BLOCK);
        border.setDamageSafeZone(DEFAULT_DAMAGE_BUFFER);
        border.setWarningBlocks(DEFAULT_WARNING_BLOCKS);
        border.setWarningTime(DEFAULT_WARNING_TIME);
    }
}
