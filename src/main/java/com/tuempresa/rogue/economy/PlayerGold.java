package com.tuempresa.rogue.economy;

import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Stores the persistent gold balance for a player.
 */
public class PlayerGold implements INBTSerializable<CompoundTag> {
    private static final String GOLD_KEY = "Gold";

    private int gold;

    public int getGold() {
        return gold;
    }

    public void setGold(int amount) {
        gold = Math.max(0, amount);
    }

    public void addGold(int amount) {
        if (amount <= 0) {
            return;
        }
        setGold(gold + amount);
    }

    public boolean removeGold(int amount) {
        if (amount <= 0) {
            return true;
        }
        if (!hasGold(amount)) {
            return false;
        }
        setGold(gold - amount);
        return true;
    }

    public boolean hasGold(int amount) {
        return amount <= 0 || gold >= amount;
    }

    public void copyFrom(PlayerGold other) {
        setGold(other.getGold());
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(GOLD_KEY, gold);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        setGold(nbt.getInt(GOLD_KEY));
    }
}
