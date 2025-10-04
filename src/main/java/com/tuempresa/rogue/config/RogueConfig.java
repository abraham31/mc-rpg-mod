package com.tuempresa.rogue.config;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.config.ModConfigSpec;

import java.util.List;

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

    public static double awakeningDropChanceCommon() {
        return COMMON.awakeningDropChanceCommon.get();
    }

    public static double awakeningDropChanceBoss() {
        return COMMON.awakeningDropChanceBoss.get();
    }

    public static int portalTierraGoldMin() {
        return COMMON.portalTierraGoldMin.get();
    }

    public static int portalTierraGoldMax() {
        int min = portalTierraGoldMin();
        return Math.max(min, COMMON.portalTierraGoldMax.get());
    }

    public static BlockPos cityPortalPos() {
        List<? extends Integer> raw = COMMON.cityPortalPos.get();
        int x = raw.size() > 0 ? raw.get(0) : 0;
        int y = raw.size() > 1 ? raw.get(1) : 64;
        int z = raw.size() > 2 ? raw.get(2) : 0;
        return new BlockPos(x, y, z);
    }

    public static ResourceLocation cityPortalDungeon() {
        ResourceLocation parsed = ResourceLocation.tryParse(COMMON.cityPortalDungeon.get());
        return parsed != null ? parsed : new ResourceLocation("rogue", "portal_tierra");
    }

    public static final class Common {
        final ModConfigSpec.IntValue affinityBonusPercent;
        final ModConfigSpec.IntValue maxAliveDefault;
        final ModConfigSpec.IntValue roomTimeLimitSeconds;
        final ModConfigSpec.BooleanValue logVerbose;
        final ModConfigSpec.DoubleValue awakeningDropChanceCommon;
        final ModConfigSpec.DoubleValue awakeningDropChanceBoss;
        final ModConfigSpec.IntValue portalTierraGoldMin;
        final ModConfigSpec.IntValue portalTierraGoldMax;
        final ModConfigSpec.ConfigValue<List<? extends Integer>> cityPortalPos;
        final ModConfigSpec.ConfigValue<String> cityPortalDungeon;

        Common(ModConfigSpec.Builder builder) {
            builder.comment("Afinidades").push("combat");
            affinityBonusPercent = builder.defineInRange("affinityBonusPercent", 25, 0, 200);
            builder.pop();

            builder.comment("Valores por defecto de mazmorras").push("dungeons");
            maxAliveDefault = builder.defineInRange("maxAliveDefault", 12, 1, 100);
            roomTimeLimitSeconds = builder.defineInRange("roomTimeLimitSeconds", 300, 10, 3600);
            builder.pop();

            builder.comment("Botín y recompensas").push("rewards");
            builder.comment("Probabilidad de que caigan orbes de despertar al derrotar mobs").push("awakening");
            awakeningDropChanceCommon = builder.defineInRange("awakeningDropChanceCommon", 0.15D, 0.0D, 1.0D);
            awakeningDropChanceBoss = builder.defineInRange("awakeningDropChanceBoss", 0.15D, 0.0D, 1.0D);
            builder.pop();

            builder.comment("Recompensas de oro específicas del portal de Tierra").push("portalTierra");
            portalTierraGoldMin = builder.defineInRange("goldMin", 180, 0, 10000);
            portalTierraGoldMax = builder.defineInRange("goldMax", 260, 0, 10000);
            builder.pop();
            builder.pop();

            builder.comment("Configuración del mundo hub").push("hub");
            cityPortalPos = builder.defineList("cityPortalPos", List.of(0, 65, 0), value ->
                value instanceof Integer integer && integer >= -30000000 && integer <= 30000000);
            cityPortalDungeon = builder.define("cityPortalDungeon", "rogue:portal_tierra", value ->
                value instanceof String str && ResourceLocation.isValidResourceLocation(str));
            builder.pop();

            builder.comment("Diagnóstico").push("debug");
            logVerbose = builder.define("logVerbose", true);
            builder.pop();
        }
    }
}
