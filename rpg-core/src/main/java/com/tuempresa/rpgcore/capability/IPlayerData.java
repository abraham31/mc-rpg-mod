package com.tuempresa.rpgcore.capability;

public interface IPlayerData {
    int getClassId();

    void setClassId(int classId);

    int getLevel();

    void setLevel(int level);

    long getXp();

    void setXp(long xp);

    long getCurrency();

    void setCurrency(long currency);

    void addXp(long amount);

    void addCurrency(long amount);
}
