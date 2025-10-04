package com.tuempresa.rogue.data.model;

import com.google.gson.JsonObject;
import com.tuempresa.rogue.config.RogueConfig;
import com.tuempresa.rogue.core.RogueConstants;
import com.tuempresa.rogue.data.DungeonDataException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public final class RewardsDef {
    private final int goldMin;
    private final int goldMax;
    private final ResourceLocation materialId;
    private final int materialMin;
    private final int materialMax;
    private final double cosmeticChance;

    public RewardsDef(int goldMin, int goldMax, ResourceLocation materialId, int materialMin, int materialMax, double cosmeticChance) {
        this.goldMin = goldMin;
        this.goldMax = goldMax;
        this.materialId = materialId;
        this.materialMin = materialMin;
        this.materialMax = materialMax;
        this.cosmeticChance = cosmeticChance;
    }

    public int goldMin() {
        return goldMin;
    }

    public int goldMax() {
        return goldMax;
    }

    public ResourceLocation materialId() {
        return materialId;
    }

    public int materialMin() {
        return materialMin;
    }

    public int materialMax() {
        return materialMax;
    }

    public double cosmeticChance() {
        return cosmeticChance;
    }

    public static RewardsDef fromJson(ResourceLocation dungeonId, JsonObject json) {
        int goldMin = Math.max(0, GsonHelper.getAsInt(json, "goldMin", 0));
        int goldMax = Math.max(goldMin, GsonHelper.getAsInt(json, "goldMax", goldMin));

        String material = GsonHelper.getAsString(json, "materialId", "minecraft:stone");
        ResourceLocation materialId = ResourceLocation.tryParse(material);
        if (materialId == null) {
            throw new DungeonDataException("Material inválido: " + material);
        }

        int materialMin = Math.max(0, GsonHelper.getAsInt(json, "materialMin", 0));
        int materialMax = Math.max(materialMin, GsonHelper.getAsInt(json, "materialMax", materialMin));
        double cosmeticChance = Math.max(0.0, GsonHelper.getAsDouble(json, "cosmeticChance", 0.0));

        if (dungeonId.equals(RogueConstants.id("portal_tierra"))) {
            int configMin = RogueConfig.portalTierraGoldMin();
            int configMax = RogueConfig.portalTierraGoldMax();
            goldMin = Math.max(0, configMin);
            goldMax = Math.max(goldMin, configMax);
        }

        return new RewardsDef(goldMin, goldMax, materialId, materialMin, materialMax, cosmeticChance);
    }
}
