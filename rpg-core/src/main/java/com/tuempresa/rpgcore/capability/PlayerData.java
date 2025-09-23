package com.tuempresa.rpgcore.capability;

import java.util.Objects;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import com.tuempresa.rpgcore.ModRpgCore;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

public class PlayerData implements IPlayerData {
    private static final Marker MARKER = MarkerManager.getMarker("PlayerData");

    private String classId = "";
    private int level = 1;
    private long xp;
    private long currency;

    public PlayerData() {
        this.level = 1;
    }

    @Override
    public String getClassId() {
        return classId;
    }

    @Override
    public void setClass(String classId) {
        if (classId == null || classId.isBlank()) {
            this.classId = "";
            return;
        }
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
        tag.putString("classId", classId);
        tag.putInt("level", level);
        tag.putLong("xp", xp);
        tag.putLong("currency", currency);
        return tag;
    }

    public void loadNBT(CompoundTag tag) {
        String storedClassId = "";
        if (tag.contains("classId", Tag.TAG_STRING)) {
            storedClassId = tag.getString("classId");
        } else if (tag.contains("ClassId")) {
            storedClassId = Integer.toString(tag.getInt("ClassId"));
        }
        setClass(storedClassId);
        int storedLevel = tag.contains("level") ? tag.getInt("level") : tag.getInt("Level");
        long storedXp = tag.contains("xp") ? tag.getLong("xp") : tag.getLong("Xp");
        long storedCurrency = tag.contains("currency") ? tag.getLong("currency") : tag.getLong("Currency");
        setLevel(Math.max(1, storedLevel));
        setXp(Math.max(0L, storedXp));
        setCurrency(Math.max(0L, storedCurrency));
    }

    public void copyFrom(IPlayerData other) {
        Objects.requireNonNull(other, "Player data to copy cannot be null");
        setClass(other.getClassId());
        setLevel(other.getLevel());
        setXp(other.getXp());
        setCurrency(other.getCurrency());
    }

    public static PlayerData get(Player player) {
        PlayerData data = player.getData(PlayerDataAttachment.TYPE);
        if (data == null) {
            throw new IllegalStateException("Missing PlayerData attachment for player " + player.getGameProfile().getName());
        }
        return data;
    }

    private static long xpToNext(int currentLevel) {
        return currentLevel * 100L;
    }
}
