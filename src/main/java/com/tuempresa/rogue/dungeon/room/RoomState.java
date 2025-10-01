package com.tuempresa.rogue.dungeon.room;

import com.tuempresa.rogue.data.model.RoomDef;
import com.tuempresa.rogue.util.AABBUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;

public final class RoomState {
    private final RoomDef def;
    private final AABB bounds;
    private final RandomSource random = RandomSource.create();
    private int alive;
    private int nextWaveAt;

    public RoomState(RoomDef def) {
        this.def = def;
        this.bounds = AABBUtil.fromInts(def.boundsMin(), def.boundsMax());
    }

    public RoomDef getDef() {
        return def;
    }

    public AABB getBounds() {
        return bounds;
    }

    public RandomSource getRandom() {
        return random;
    }

    public int getAlive() {
        return alive;
    }

    public void setAlive(int alive) {
        this.alive = alive;
    }

    public int getNextWaveAt() {
        return nextWaveAt;
    }

    public void scheduleNextWave(int ticks) {
        this.nextWaveAt = ticks;
    }

    public boolean hasPlayersInside() {
        return alive > 0;
    }
}
