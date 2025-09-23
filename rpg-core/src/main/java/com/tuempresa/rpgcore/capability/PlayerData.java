package com.tuempresa.rpgcore.capability;

import java.util.Objects;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import com.tuempresa.rpgcore.ModRpgCore;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public class PlayerData implements IPlayerData {
    private static final Marker MARKER = MarkerManager.getMarker("PlayerData");

    private int classId;
    private int level = 1;
    private long xp;
    private long currency;

    public PlayerData() {
        this.level = 1;
    }

    @Override
    public int getClassId() {
        return classId;
    }

    @Override
    public void setClassId(int classId) {
        this.classId = classId;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public void setLevel(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("Level must be at least 1");
        }
        this.level = level;
    }

    @Override
    public long getXp() {
        return xp;
    }

    @Override
    public void setXp(long xp) {
        if (xp < 0) {
            throw new IllegalArgumentException("XP cannot be negative");
        }
        this.xp = xp;
    }

    @Override
    public long getCurrency() {
        return currency;
    }

    @Override
    public void setCurrency(long currency) {
        if (currency < 0) {
            throw new IllegalArgumentException("Currency cannot be negative");
        }
        this.currency = currency;
    }

    @Override
    public void addXp(long amount) {
        if (amount <= 0) {
            return;
        }

        xp += amount;
        while (xp >= xpToNext(level)) {
            xp -= xpToNext(level);
            level++;
            ModRpgCore.LOG.info(MARKER, "Player leveled up to {}", level);
        }
    }

    @Override
    public void addCurrency(long amount) {
        if (amount <= 0) {
            return;
        }
        setCurrency(currency + amount);
    }

    public CompoundTag saveNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("ClassId", classId);
        tag.putInt("Level", level);
        tag.putLong("Xp", xp);
        tag.putLong("Currency", currency);
        return tag;
    }

    public void loadNBT(CompoundTag tag) {
        setClassId(tag.getInt("ClassId"));
        setLevel(Math.max(1, tag.getInt("Level")));
        setXp(Math.max(0L, tag.getLong("Xp")));
        setCurrency(Math.max(0L, tag.getLong("Currency")));
    }

    public void copyFrom(IPlayerData other) {
        Objects.requireNonNull(other, "Player data to copy cannot be null");
        setClassId(other.getClassId());
        setLevel(other.getLevel());
        setXp(other.getXp());
        setCurrency(other.getCurrency());
    }

    public static PlayerData get(Player player) {
        IPlayerData data = player.getCapability(PlayerDataProvider.PLAYER_DATA)
            .orElseThrow(() -> new IllegalStateException("Missing PlayerData capability for player " + player.getGameProfile().getName()));
        if (data instanceof PlayerData playerData) {
            return playerData;
        }
        PlayerData copy = new PlayerData();
        copy.copyFrom(data);
        return copy;
    }

    private static long xpToNext(int currentLevel) {
        return currentLevel * 100L;
    }
}
