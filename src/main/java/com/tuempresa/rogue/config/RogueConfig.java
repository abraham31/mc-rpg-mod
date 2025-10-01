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

    public static double affinityBonusMultiplier() {
        return COMMON.affinityBonusPercent.get() / 100.0;
    }

    public static int maxAliveDefault() {
        return COMMON.maxAliveDefault.get();
    }

    public static int roomTimeLimitTicks() {
        return COMMON.roomTimeLimitSeconds.get() * 20;
    }

    public static boolean logVerbose() {
        return COMMON.logVerbose.get();
    }

    public static final class Common {
        final ModConfigSpec.IntValue affinityBonusPercent;
        final ModConfigSpec.IntValue maxAliveDefault;
        final ModConfigSpec.IntValue roomTimeLimitSeconds;
        final ModConfigSpec.BooleanValue logVerbose;

        Common(ModConfigSpec.Builder builder) {
            builder.comment("Afinidades").push("combat");
            affinityBonusPercent = builder.defineInRange("affinityBonusPercent", 25, 0, 200);
            builder.pop();

            builder.comment("Valores por defecto de mazmorras").push("dungeons");
            maxAliveDefault = builder.defineInRange("maxAliveDefault", 12, 1, 100);
            roomTimeLimitSeconds = builder.defineInRange("roomTimeLimitSeconds", 300, 10, 3600);
            builder.pop();

            builder.comment("Diagnóstico").push("debug");
            logVerbose = builder.define("logVerbose", true);
            builder.pop();
        }
    }
}
