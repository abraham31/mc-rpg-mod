package com.tuempresa.rogue.dungeon;

import com.tuempresa.rogue.data.model.DungeonDef;
import com.tuempresa.rogue.dungeon.instance.DungeonRun;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

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
        for (DungeonRun run : runsById.values()) {
            if (!run.getParty().contains(player.getUUID())) {
                continue;
            }
            run.returnToCity(player);
            return true;
        }
        return false;
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
