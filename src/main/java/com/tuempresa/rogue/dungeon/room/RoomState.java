package com.tuempresa.rogue.dungeon.room;

import com.tuempresa.rogue.data.model.RoomDef;
import com.tuempresa.rogue.util.AABBUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class RoomState {
    private final RoomDef def;
    private final AABB bounds;
    private final RandomSource random = RandomSource.create();
    private final Set<UUID> mobs = new HashSet<>();
    private int alive;
    private int nextWaveAt;
    private boolean started;
    private boolean cleared;

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

    public int getNextWaveAt() {
        return nextWaveAt;
    }

    public void scheduleNextWave(int absoluteTick) {
        this.nextWaveAt = absoluteTick;
    }

    public boolean hasPlayersInside() {
        return alive > 0;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isCleared() {
        return cleared;
    }

    public boolean isWaveReady(int currentTick) {
        return !cleared && currentTick >= nextWaveAt;
    }

    public void registerMob(UUID mobId) {
        mobs.add(mobId);
        alive = mobs.size();
        started = true;
        cleared = false;
    }

    public void unregisterMob(UUID mobId) {
        if (mobs.remove(mobId)) {
            alive = mobs.size();
            if (alive <= 0) {
                cleared = true;
            }
        }
    }

    public void resetState(int currentTick) {
        mobs.clear();
        alive = 0;
        nextWaveAt = currentTick;
        started = false;
        cleared = false;
    }

    public void clearTracked() {
        mobs.clear();
        alive = 0;
        cleared = false;
        started = false;
        nextWaveAt = 0;
    }
}
