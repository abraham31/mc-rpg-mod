package com.tuempresa.rogue.dungeon.spawn;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

import java.util.Objects;

/**
 * Weighted mob entry for dungeon wave spawning. Each entry defines a mob type,
 * its selection weight and the amount of mobs that should be produced when the
 * entry is picked by {@link SpawnSystem}.
 */
public final class MobEntry {
    private final EntityType<? extends Mob> entityType;
    private final int weight;
    private final int count;
    private final CompoundTag nbt;

    public MobEntry(EntityType<? extends Mob> entityType, int weight, int count) {
        this(entityType, weight, count, null);
    }

    public MobEntry(EntityType<? extends Mob> entityType, int weight, int count, CompoundTag nbt) {
        this.entityType = Objects.requireNonNull(entityType, "entityType");
        if (weight <= 0) {
            throw new IllegalArgumentException("weight must be > 0");
        }
        if (count <= 0) {
            throw new IllegalArgumentException("count must be > 0");
        }
        this.weight = weight;
        this.count = count;
        this.nbt = nbt != null ? nbt.copy() : null;
    }

    public EntityType<? extends Mob> entityType() {
        return entityType;
    }

    public int weight() {
        return weight;
    }

    public int count() {
        return count;
    }

    public boolean hasNbt() {
        return nbt != null && !nbt.isEmpty();
    }

    public CompoundTag nbt() {
        return nbt != null ? nbt.copy() : null;
    }
}
