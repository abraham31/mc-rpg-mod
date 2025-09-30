package com.tuempresa.rogue.dungeon.spawn;

import com.tuempresa.rogue.RogueMod;
import com.tuempresa.rogue.world.RogueDimensions;
import com.tuempresa.rogue.world.RogueEntityTypeTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles mob spawning logic for dungeon rooms.
 */
public final class SpawnSystem {
    private static final String TAG_ROGUE_MOB = "rogue_mob";
    private static final String TAG_EARTH = "EARTH";
    private static final int MAX_POSITION_ATTEMPTS_PER_MOB = 12;

    private SpawnSystem() {
    }

    public static void spawnWave(RoomState state) {
        Objects.requireNonNull(state, "state");
        ServerLevel level = state.level();
        if (state.mobEntries().isEmpty()) {
            return;
        }

        if (level.dimension() != RogueDimensions.EARTH_DUNGEON_LEVEL) {
            RogueMod.LOGGER.debug("Ignoring spawnWave outside EARTH dimension: {}", level.dimension().location());
            return;
        }

        BoundingBox bounds = state.bounds();
        int alive = countAlive(level, bounds);
        int maxAlive = state.maxAlive();
        if (alive >= maxAlive) {
            return;
        }

        int desired = state.desiredSpawnCount();
        if (desired <= 0) {
            return;
        }

        int toSpawn = Math.min(desired, maxAlive - alive);
        if (toSpawn <= 0) {
            return;
        }

        RandomSource random = state.random();
        List<MobEntry> entries = state.mobEntries();
        int size = entries.size();
        int[] remaining = new int[size];
        int spawnBudget = 0;
        for (int i = 0; i < size; i++) {
            MobEntry entry = entries.get(i);
            if (!entry.entityType().builtInRegistryHolder().is(RogueEntityTypeTags.ROGUE_MOBS)) {
                remaining[i] = 0;
                continue;
            }
            long quota = (long) entry.count() * state.partySize();
            if (quota <= 0L) {
                remaining[i] = 0;
                continue;
            }
            int clamped = (int) Math.min(Integer.MAX_VALUE, quota);
            remaining[i] = clamped;
            spawnBudget += clamped;
        }

        if (spawnBudget == 0) {
            return;
        }

        int capacity = Math.min(toSpawn, spawnBudget);
        int attempts = 0;
        int attemptLimit = Math.max(capacity * MAX_POSITION_ATTEMPTS_PER_MOB, MAX_POSITION_ATTEMPTS_PER_MOB);
        int spawned = 0;
        while (spawned < capacity && attempts < attemptLimit) {
            int totalWeight = computeWeight(entries, remaining);
            if (totalWeight <= 0) {
                break;
            }

            int index = pickEntryIndex(entries, remaining, totalWeight, random);
            if (index < 0) {
                break;
            }

            MobEntry entry = entries.get(index);
            Optional<BlockPos> spawnPos = findSpawnPosition(level, bounds, entry.entityType(), random);
            attempts++;
            if (spawnPos.isEmpty()) {
                continue;
            }

            if (spawnMob(level, spawnPos.get(), entry, random)) {
                spawned++;
                alive++;
                remaining[index]--;
                if (alive >= maxAlive) {
                    return;
                }
            }
        }
    }

    private static int computeWeight(List<MobEntry> entries, int[] remaining) {
        int total = 0;
        for (int i = 0; i < entries.size(); i++) {
            if (remaining[i] <= 0) {
                continue;
            }
            int weight = entries.get(i).weight();
            if (weight > 0) {
                total += weight;
            }
        }
        return total;
    }

    private static int pickEntryIndex(List<MobEntry> entries, int[] remaining, int totalWeight, RandomSource random) {
        if (totalWeight <= 0) {
            return -1;
        }
        int target = random.nextInt(totalWeight);
        int cumulative = 0;
        for (int i = 0; i < entries.size(); i++) {
            if (remaining[i] <= 0) {
                continue;
            }
            MobEntry entry = entries.get(i);
            int weight = entry.weight();
            if (weight <= 0) {
                continue;
            }
            cumulative += weight;
            if (target < cumulative) {
                return i;
            }
        }
        return -1;
    }

    private static boolean spawnMob(ServerLevel level, BlockPos pos, MobEntry entry, RandomSource random) {
        Mob mob = createMob(level, pos, entry, random);
        if (mob == null) {
            return false;
        }

        mob.addTag(TAG_ROGUE_MOB);
        mob.addTag(TAG_EARTH);
        mob.setPersistenceRequired();

        boolean added = level.tryAddFreshEntityWithPassengers(mob);
        if (!added) {
            mob.discard();
            return false;
        }
        return true;
    }

    private static Mob createMob(ServerLevel level, BlockPos pos, MobEntry entry, RandomSource random) {
        EntityType<? extends Mob> type = entry.entityType();
        CompoundTag data = new CompoundTag();
        ResourceLocation id = EntityType.getKey(type);
        if (id == null) {
            return null;
        }
        data.putString("id", id.toString());
        CompoundTag customData = entry.nbt();
        boolean hasCustomData = customData != null && !customData.isEmpty();
        if (hasCustomData) {
            data.merge(customData);
        }

        Entity entity = EntityType.loadEntityRecursive(data, level, existing -> {
            double x = pos.getX() + 0.5D;
            double y = pos.getY();
            double z = pos.getZ() + 0.5D;
            existing.moveTo(x, y, z, random.nextFloat() * 360.0F, 0.0F);
            return existing;
        });

        if (!(entity instanceof Mob mob)) {
            if (entity != null) {
                entity.discard();
            }
            return null;
        }

        if (!mob.getType().builtInRegistryHolder().is(RogueEntityTypeTags.ROGUE_MOBS)) {
            mob.discard();
            return null;
        }

        if (!hasCustomData) {
            DifficultyInstance difficulty = level.getCurrentDifficultyAt(pos);
            mob.finalizeSpawn(level, difficulty, MobSpawnType.EVENT, null);
        }

        mob.setUUID(UUID.randomUUID());
        mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
        return mob;
    }

    private static int countAlive(ServerLevel level, BoundingBox bounds) {
        AABB box = new AABB(bounds.minX(), bounds.minY(), bounds.minZ(),
            bounds.maxX() + 1, bounds.maxY() + 1, bounds.maxZ() + 1);
        return level.getEntitiesOfClass(Mob.class, box, mob -> mob.isAlive() && mob.getTags().contains(TAG_ROGUE_MOB)).size();
    }

    private static Optional<BlockPos> findSpawnPosition(ServerLevel level,
                                                        BoundingBox bounds,
                                                        EntityType<? extends Mob> type,
                                                        RandomSource random) {
        int minX = bounds.minX();
        int minY = Math.max(bounds.minY(), level.getMinBuildHeight());
        int minZ = bounds.minZ();
        int maxX = bounds.maxX();
        int maxY = Math.min(bounds.maxY(), level.getMaxBuildHeight() - 1);
        int maxZ = bounds.maxZ();
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            return Optional.empty();
        }

        for (int attempt = 0; attempt < MAX_POSITION_ATTEMPTS_PER_MOB; attempt++) {
            int x = Mth.nextInt(random, minX, maxX);
            int z = Mth.nextInt(random, minZ, maxZ);
            int startY = Mth.nextInt(random, minY, maxY);
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, startY, z);
            if (!level.getWorldBorder().isWithinBounds(cursor)) {
                continue;
            }

            Optional<BlockPos> found = findGroundPosition(level, cursor, minY, maxY, type);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private static Optional<BlockPos> findGroundPosition(ServerLevel level,
                                                         BlockPos.MutableBlockPos cursor,
                                                         int minY,
                                                         int maxY,
                                                         EntityType<? extends Mob> type) {
        int start = Mth.clamp(cursor.getY(), minY, maxY);
        for (int y = start; y >= minY; y--) {
            cursor.setY(y);
            BlockState state = level.getBlockState(cursor);
            if (!state.isAir() && state.isFaceSturdy(level, cursor, Direction.UP)) {
                BlockPos spawnPos = cursor.above();
                if (spawnPos.getY() > maxY) {
                    continue;
                }
                if (isSpawnSpaceValid(level, spawnPos, type)) {
                    return Optional.of(spawnPos);
                }
            }
        }
        return Optional.empty();
    }

    private static boolean isSpawnSpaceValid(ServerLevel level, BlockPos pos, EntityType<? extends Mob> type) {
        if (!level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        BlockState above = level.getBlockState(pos.above());
        if (!state.isAir() || !above.isAir()) {
            return false;
        }
        AABB aabb = type.getDimensions().makeBoundingBox(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        if (!level.noCollision(aabb)) {
            return false;
        }
        return level.getFluidState(pos).isEmpty() && level.getFluidState(pos.above()).isEmpty();
    }
}
