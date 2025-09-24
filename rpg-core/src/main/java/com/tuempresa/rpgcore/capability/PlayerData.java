package com.tuempresa.rpgcore.capability;

import net.minecraft.nbt.CompoundTag;

public class PlayerData {
  private String classId = "none";
  private int level = 1;
  private long xp = 0;
  private long currency = 0;

  public String getClassId() { return classId; }
  public int getLevel() { return level; }
  public long getXp() { return xp; }
  public long getCurrency() { return currency; }

  public void setClassId(String id) { this.classId = id == null ? "none" : id; }
  public void setLevel(int lvl) { this.level = Math.max(1, lvl); }
  public void addXp(long amount) {
    if (amount <= 0) return;
    xp += amount;
    while (xp >= xpToNext(level)) {
      xp -= xpToNext(level);
      level++;
    }
  }
  public void addCurrency(long amount){ if (amount>0) currency += amount; }

  private long xpToNext(int n){ return n * 100L; }

  // Persistencia
  public CompoundTag saveNBT() {
    CompoundTag tag = new CompoundTag();
    tag.putString("classId", classId);
    tag.putInt("level", level);
    tag.putLong("xp", xp);
    tag.putLong("currency", currency);
    return tag;
  }
  public void loadNBT(CompoundTag tag) {
    classId = tag.getString("classId");
    level = Math.max(1, tag.getInt("level"));
    xp = Math.max(0, tag.getLong("xp"));
    currency = Math.max(0, tag.getLong("currency"));
  }
}
