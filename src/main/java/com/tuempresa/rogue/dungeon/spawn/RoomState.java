package com.tuempresa.rogue.dungeon.spawn;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable snapshot describing the context required for spawning a wave
 * inside a dungeon room.
 */
public final class RoomState {
    private final ServerLevel level;
    private final BoundingBox bounds;
    private final List<MobEntry> mobEntries;
    private final int maxAlive;
    private final int partySize;
    private final RandomSource random;

    public RoomState(ServerLevel level,
                     BoundingBox bounds,
                     List<MobEntry> mobEntries,
                     int maxAlive,
                     int partySize) {
        this(level, bounds, mobEntries, maxAlive, partySize, null);
    }

    public RoomState(ServerLevel level,
                     BoundingBox bounds,
                     List<MobEntry> mobEntries,
                     int maxAlive,
                     int partySize,
                     RandomSource random) {
        this.level = Objects.requireNonNull(level, "level");
        this.bounds = Objects.requireNonNull(bounds, "bounds");
        if (mobEntries == null || mobEntries.isEmpty()) {
            throw new IllegalArgumentException("mobEntries must not be empty");
        }
        this.mobEntries = Collections.unmodifiableList(new ArrayList<>(mobEntries));
        if (maxAlive <= 0) {
            throw new IllegalArgumentException("maxAlive must be > 0");
        }
        this.maxAlive = maxAlive;
        this.partySize = Math.max(1, partySize);
        this.random = random != null ? random : RandomSource.create();
    }

    public ServerLevel level() {
        return level;
    }

    public BoundingBox bounds() {
        return bounds;
    }

    public List<MobEntry> mobEntries() {
        return mobEntries;
    }

    public int maxAlive() {
        return maxAlive;
    }

    public int partySize() {
        return partySize;
    }

    public RandomSource random() {
        return random;
    }

    /**
     * Number of mobs the wave should attempt to spawn after factoring the
     * party size scaling.
     */
    public int desiredSpawnCount() {
        long total = 0L;
        for (MobEntry entry : mobEntries) {
            total += (long) entry.count() * partySize;
        }
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, total));
    }
}
