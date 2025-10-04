package com.tuempresa.rogue.reward.awakening;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.util.Mth;

import java.util.UUID;

public final class ArmorAwakening {
    private static final String NBT_KEY = "RogueArmorAwakening";
    private static final UUID MODIFIER_ID = UUID.fromString("5b985c7b-06ba-46df-9bf5-b831bdb9fee7");
    private static final double BONUS_PER_LEVEL = 2.0D;
    private static final int MAX_LEVEL = 3;

    private ArmorAwakening() {
    }

    public static boolean nextLevel(ServerPlayer player) {
        int current = getLevel(player);
        if (current >= MAX_LEVEL) {
            return false;
        }
        int next = current + 1;
        setLevel(player, next);
        return true;
    }

    public static int setLevel(ServerPlayer player, int level) {
        int clamped = Mth.clamp(level, 0, MAX_LEVEL);
        writeLevel(player, clamped);
        applyModifier(player, clamped);
        return clamped;
    }

    public static void reset(ServerPlayer player) {
        setLevel(player, 0);
    }

    public static int getLevel(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        return data.getInt(NBT_KEY);
    }

    private static void writeLevel(ServerPlayer player, int level) {
        CompoundTag data = player.getPersistentData();
        data.putInt(NBT_KEY, level);
    }

    private static void applyModifier(ServerPlayer player, int level) {
        AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
        if (armor == null) {
            return;
        }
        armor.removePermanentModifier(MODIFIER_ID);
        armor.removeModifier(MODIFIER_ID);
        if (level <= 0) {
            return;
        }
        double bonus = BONUS_PER_LEVEL * level;
        AttributeModifier modifier = new AttributeModifier(
            MODIFIER_ID,
            "RogueArmorAwakening",
            bonus,
            AttributeModifier.Operation.ADD_VALUE
        );
        armor.addPermanentModifier(modifier);
    }
}
