package com.tuempresa.rogue.config;

import net.neoforged.fml.config.ModConfigSpec;

/**
 * Centraliza la configuración común del mod y expone accesos convenientes
 * para los subsistemas que dependen de parámetros ajustables.
 */
public final class RogueConfig {
    public static final ModConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        COMMON = new Common(builder);
        COMMON_SPEC = builder.build();
    }

    private RogueConfig() {
    }

    public static Common common() {
        return COMMON;
    }

    public static int roomClearThresholdTicks() {
        return Math.max(1, COMMON.roomClearThresholdTicks.get());
    }

    public static int roomSpacingBlocks() {
        return Math.max(1, COMMON.roomSpacingBlocks.get());
    }

    public static boolean logRunLifecycle() {
        return COMMON.logRunLifecycle.get();
    }

    public static boolean logRoomLifecycle() {
        return COMMON.logRoomLifecycle.get();
    }

    public static boolean logSpawnLifecycle() {
        return COMMON.logSpawnLifecycle.get();
    }

    public static final class Common {
        final ModConfigSpec.IntValue roomClearThresholdTicks;
        final ModConfigSpec.IntValue roomSpacingBlocks;
        final ModConfigSpec.BooleanValue logRunLifecycle;
        final ModConfigSpec.BooleanValue logRoomLifecycle;
        final ModConfigSpec.BooleanValue logSpawnLifecycle;

        Common(ModConfigSpec.Builder builder) {
            builder.comment("Parámetros generales de las mazmorras").push("dungeons");
            roomClearThresholdTicks = builder
                .comment("Ticks consecutivos necesarios para marcar una sala como despejada.")
                .defineInRange("roomClearThresholdTicks", 40, 1, 20_000);
            roomSpacingBlocks = builder
                .comment("Separación en bloques entre salas consecutivas dentro de la dimensión de mazmorras.")
                .defineInRange("roomSpacingBlocks", 20, 1, 512);
            builder.pop();

            builder.comment("Control de verbosidad de logs").push("logs");
            logRunLifecycle = builder
                .comment("Registra eventos de inicio/fin de partidas en el log de depuración.")
                .define("logRunLifecycle", true);
            logRoomLifecycle = builder
                .comment("Registra transiciones de salas y limpieza de habitaciones en el log de depuración.")
                .define("logRoomLifecycle", true);
            logSpawnLifecycle = builder
                .comment("Registra los detalles de aparición de waves y mobs en el log de depuración.")
                .define("logSpawnLifecycle", false);
            builder.pop();
        }
    }
}
