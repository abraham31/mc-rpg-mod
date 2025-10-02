package com.tuempresa.rogue.dungeon;

import com.tuempresa.rogue.data.model.DungeonDef;
import com.tuempresa.rogue.dungeon.instance.DungeonRun;
import com.tuempresa.rogue.dungeon.room.RoomState;
import com.tuempresa.rogue.util.AABBUtil;
import com.tuempresa.rogue.util.TP;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DungeonManager {
    private static final Map<ResourceLocation, DungeonRun> runsByDungeon = new ConcurrentHashMap<>();
    private static final Map<UUID, DungeonRun> runsById = new ConcurrentHashMap<>();

    private DungeonManager() {
    }

    public static DungeonRun createOrJoin(DungeonDef def, ServerPlayer player) {
        DungeonRun run = runsByDungeon.computeIfAbsent(def.id(), id -> new DungeonRun(def));
        runsById.putIfAbsent(run.getId(), run);
        run.addMember(player);
        return run;
    }

    public static Optional<DungeonRun> getRun(UUID runId) {
        return Optional.ofNullable(runsById.get(runId));
    }

    public static Optional<DungeonRun> findRunByPlayer(UUID playerId) {
        return runsById.values().stream().filter(run -> run.hasMember(playerId)).findFirst();
    }

    public static List<RunInfo> listRuns() {
        List<RunInfo> info = new ArrayList<>();
        runsById.values().forEach(run -> info.add(new RunInfo(run.getId(), run.getDef().id(), run.getParty().size(), run.isVictory())));
        return info;
    }

    public static int cleanupFinishedRuns() {
        List<UUID> toRemove = new ArrayList<>();
        runsById.forEach((id, run) -> {
            if (run.isFinished()) {
                toRemove.add(id);
            }
        });
        toRemove.forEach(id -> {
            DungeonRun run = runsById.remove(id);
            if (run != null) {
                runsByDungeon.remove(run.getDef().id());
            }
        });
        return toRemove.size();
    }

    public static boolean warpPlayerToRoom(ServerPlayer player, String roomId) {
        if (player.server == null) {
            return false;
        }

        Optional<DungeonRun> runOptional = findRunByPlayer(player.getUUID());
        if (runOptional.isEmpty()) {
            return false;
        }

        DungeonRun run = runOptional.get();
        RoomState room = run.findRoomById(roomId);
        if (room == null) {
            return false;
        }

        ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, run.getDef().world());
        ServerLevel level = player.server.getLevel(levelKey);
        if (level == null) {
            return false;
        }

        AABB bounds = room.getBounds();
        for (int attempt = 0; attempt < 20; attempt++) {
            BlockPos candidate = AABBUtil.randomPosInside(bounds, room.getRandom());
            if (!isValidTeleportPosition(level, candidate)) {
                continue;
            }
            teleportPlayer(player, levelKey, candidate);
            return true;
        }

        BlockPos fallback = findFirstValidPosition(level, bounds);
        if (fallback != null) {
            teleportPlayer(player, levelKey, fallback);
            return true;
        }

        return false;
    }

    private static boolean isValidTeleportPosition(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        return !level.isEmptyBlock(below) && level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above());
    }

    private static BlockPos findFirstValidPosition(ServerLevel level, AABB bounds) {
        int minX = (int) Math.floor(bounds.minX);
        int maxX = (int) Math.ceil(bounds.maxX);
        int minY = (int) Math.floor(bounds.minY);
        int maxY = (int) Math.ceil(bounds.maxY);
        int minZ = (int) Math.floor(bounds.minZ);
        int maxZ = (int) Math.ceil(bounds.maxZ);

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int y = maxY - 1; y >= minY; y--) {
            for (int x = minX; x < maxX; x++) {
                for (int z = minZ; z < maxZ; z++) {
                    mutable.set(x, y, z);
                    if (isValidTeleportPosition(level, mutable)) {
                        return mutable.immutable();
                    }
                }
            }
        }
        return null;
    }

    private static void teleportPlayer(ServerPlayer player, ResourceKey<Level> levelKey, BlockPos pos) {
        Vec3 target = new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        TP.to(player, levelKey, target);
    }

    public static void onServerTick(MinecraftServer server) {
        List<UUID> completed = new ArrayList<>();
        runsById.forEach((id, run) -> {
            run.tick(server);
            if (run.isFinished()) {
                completed.add(id);
            }
        });
        completed.forEach(id -> {
            DungeonRun run = runsById.remove(id);
            if (run != null) {
                runsByDungeon.remove(run.getDef().id());
            }
        });
    }

    public record RunInfo(UUID runId, ResourceLocation dungeonId, int partySize, boolean victory) {
    }
}
