package com.tuempresa.rogue.reward.awakening;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public final class WeaponAwakening {
    private static final String NBT_KEY = "RogueWeaponAwakening";
    private static final UUID MODIFIER_ID = UUID.fromString("7c7ae7f1-8105-4c07-9a9c-5191d66efd9e");
    private static final double BONUS_PER_LEVEL = 1.5D;
    private static final int MAX_LEVEL = 3;

    private WeaponAwakening() {
    }

    public static boolean nextLevel(ServerPlayer player) {
        int current = getLevel(player);
        if (current >= MAX_LEVEL) {
            return false;
        }
        int next = current + 1;
        setLevel(player, next);
        applyModifier(player, next);
        return true;
    }

    public static void reset(ServerPlayer player) {
        setLevel(player, 0);
        applyModifier(player, 0);
    }

    private static int getLevel(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        return data.getInt(NBT_KEY);
    }

    private static void setLevel(ServerPlayer player, int level) {
        CompoundTag data = player.getPersistentData();
        data.putInt(NBT_KEY, level);
    }

    private static void applyModifier(ServerPlayer player, int level) {
        AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack == null) {
            return;
        }
        attack.removePermanentModifier(MODIFIER_ID);
        attack.removeModifier(MODIFIER_ID);
        if (level <= 0) {
            return;
        }
        double bonus = BONUS_PER_LEVEL * level;
        AttributeModifier modifier = new AttributeModifier(
            MODIFIER_ID,
            "RogueWeaponAwakening",
            bonus,
            AttributeModifier.Operation.ADD_VALUE
        );
        attack.addPermanentModifier(modifier);
    }
}
