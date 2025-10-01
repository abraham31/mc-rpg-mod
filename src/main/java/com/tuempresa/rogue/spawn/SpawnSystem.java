package com.tuempresa.rogue.spawn;

import com.tuempresa.rogue.data.model.DungeonDef;
import com.tuempresa.rogue.data.model.MobEntry;
import com.tuempresa.rogue.dungeon.room.RoomState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

import java.util.Optional;

public final class SpawnSystem {
    private SpawnSystem() {
    }

    public static void spawnWave(ServerLevel level, RoomState state, DungeonDef def) {
        state.setAlive(state.getAlive() + 1);
    }

    public static MobEntry pickWeighted(MobEntry[] entries) {
        return entries.length > 0 ? entries[0] : null;
    }

    public static Optional<Mob> spawnMob(ServerLevel level, MobEntry entry) {
        EntityType<?> type = EntityType.byString(entry.id().toString()).orElse(null);
        if (type instanceof EntityType<? extends Mob> mobType) {
            return Optional.ofNullable(mobType.create(level));
        }
        return Optional.empty();
    }

    public static void applyTags(Mob mob, String... tags) {
        for (String tag : tags) {
            mob.addTag(tag);
        }
    }

    public static void scaleStatsByParty(Mob mob, int partySize) {
    }
}
