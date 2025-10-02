package com.tuempresa.rogue.spawn;

import com.tuempresa.rogue.data.model.MobEntry;
import com.tuempresa.rogue.data.model.WaveDef;
import com.tuempresa.rogue.dungeon.instance.DungeonRun;
import com.tuempresa.rogue.dungeon.room.RoomState;
import com.tuempresa.rogue.util.AABBUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class SpawnSystem {
    private SpawnSystem() {
    }

    public static void spawnWave(ServerLevel level, DungeonRun run, RoomState state, int roomIndex) {
        WaveDef wave = state.getDef().wave();
        if (wave == null) {
            return;
        }

        int missing = Math.max(0, wave.maxAlive() - state.getAlive());
        if (missing <= 0) {
            return;
        }

        RandomSource random = state.getRandom();
        for (int i = 0; i < missing; i++) {
            MobEntry entry = pickWeighted(wave.packs(), random);
            if (entry == null) {
                continue;
            }
            Optional<Mob> optionalMob = spawnMob(level, entry, state, random);
            optionalMob.ifPresent(mob -> {
                mob.setPersistenceRequired();
                applyTags(mob, "rogue_dungeon", "rogue_run:" + run.getId());
                String affinity = state.getDef().affinityTag();
                if (!affinity.isBlank()) {
                    applyTags(mob, "affinity:" + affinity);
                }
                scaleStatsByParty(mob, run.getParty().size());
                level.addFreshEntity(mob);
                run.registerMob(mob.getUUID(), roomIndex);
            });
        }
    }

    public static MobEntry pickWeighted(List<MobEntry> entries, RandomSource random) {
        if (entries.isEmpty()) {
            return null;
        }
        int totalWeight = entries.stream().mapToInt(MobEntry::weight).sum();
        if (totalWeight <= 0) {
            return entries.get(0);
        }
        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (MobEntry entry : entries) {
            cumulative += entry.weight();
            if (roll < cumulative) {
                return entry;
            }
        }
        return entries.get(entries.size() - 1);
    }

    public static Optional<Mob> spawnMob(ServerLevel level, MobEntry entry, RoomState state, RandomSource random) {
        EntityType<?> type = EntityType.byString(entry.id().toString()).orElse(null);
        if (type instanceof EntityType<? extends Mob> mobType) {
            Mob mob = mobType.create(level);
            if (mob == null) {
                return Optional.empty();
            }
            BlockPos pos = AABBUtil.randomPosInside(state.getBounds(), random);
            mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), net.minecraft.world.entity.MobSpawnType.EVENT, null);
            if (!entry.nbt().isEmpty()) {
                mob.readAdditionalSaveData(entry.nbt().copy());
            }
            return Optional.of(mob);
        }
        return Optional.empty();
    }

    public static void applyTags(Mob mob, String... tags) {
        for (String tag : tags) {
            if (tag != null && !tag.isBlank()) {
                mob.addTag(tag);
            }
        }
    }

    public static void scaleStatsByParty(Mob mob, int partySize) {
        if (partySize <= 1) {
            return;
        }
        double multiplier = 1.0D + 0.25D * (partySize - 1);
        AttributeInstance maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(maxHealth.getBaseValue() * multiplier);
            mob.setHealth((float) maxHealth.getValue());
        }
        AttributeInstance attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.setBaseValue(attack.getBaseValue() * multiplier);
        }
    }

    public static void cleanup(ServerLevel level, Set<UUID> mobs) {
        mobs.forEach(id -> {
            var entity = level.getEntity(id);
            if (entity != null) {
                entity.discard();
            }
        });
    }
}
