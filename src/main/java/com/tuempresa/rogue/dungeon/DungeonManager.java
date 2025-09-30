package com.tuempresa.rogue.dungeon;

import com.tuempresa.rogue.RogueMod;
import com.tuempresa.rogue.config.RogueConfig;
import com.tuempresa.rogue.data.model.DungeonDef;
import com.tuempresa.rogue.data.model.PortalDef;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps track of active dungeon runs and ensures players are grouped in a
 * deterministic fashion when entering through a portal.
 */
@Mod.EventBusSubscriber(modid = RogueMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DungeonManager {
    private static final DungeonManager INSTANCE = new DungeonManager();

    private final Map<ResourceLocation, DungeonRun> activeRuns = new ConcurrentHashMap<>();
    private final Map<UUID, DungeonRun> runsById = new ConcurrentHashMap<>();

    private DungeonManager() {
    }

    public static DungeonInstance createOrJoin(ServerPlayer player, PortalDef portal) {
        return INSTANCE.createOrJoinInternal(player, portal);
    }

    private DungeonInstance createOrJoinInternal(ServerPlayer player, PortalDef portal) {
        ResourceLocation dungeonId = portal.dungeonId();
        DungeonDef dungeonDef = RogueMod.DUNGEON_DATA.dungeons().get(dungeonId);
        if (dungeonDef == null) {
            throw new IllegalStateException("No existe la definición de la mazmorra " + dungeonId);
        }

        DungeonRun run = activeRuns.compute(dungeonId, (id, existing) -> {
            DungeonRun current = existing;
            if (current == null) {
                current = new DungeonRun(new DungeonInstance(id), dungeonDef);
                runsById.put(current.runId(), current);
            }
            return current;
        });

        if (run == null) {
            throw new IllegalStateException("No se pudo crear la instancia para la mazmorra " + dungeonId);
        }

        runsById.putIfAbsent(run.runId(), run);
        run.join(player);
        return run.instance();
    }

    private void tick(MinecraftServer server) {
        Iterator<Map.Entry<ResourceLocation, DungeonRun>> iterator = activeRuns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ResourceLocation, DungeonRun> entry = iterator.next();
            DungeonRun run = entry.getValue();
            DungeonRun.TickResult result = run.tick(server);
            if (result.completed()) {
                run.finish(server, result.victory());
                if (RogueConfig.logRunLifecycle()) {
                    RogueMod.LOGGER.debug("Finalizada la instancia de la mazmorra {}", entry.getKey());
                }
                iterator.remove();
                runsById.remove(run.runId());
            }
        }
    }

    static void onMobDefeated(UUID runId, UUID mobId) {
        DungeonRun run = INSTANCE.runsById.get(runId);
        if (run != null) {
            run.onMobDefeated(mobId);
        }
    }

    static Optional<DungeonRun> findRun(UUID runId) {
        return Optional.ofNullable(INSTANCE.runsById.get(runId));
    }

    public static List<RunInfo> listRuns() {
        return INSTANCE.listRunsInternal();
    }

    public static Optional<RunInfo> findRunForPlayer(UUID playerId) {
        return INSTANCE.findRunForPlayerInternal(playerId);
    }

    public static boolean warpPlayerToRoom(ServerPlayer player, int roomIndex) {
        return INSTANCE.warpPlayerToRoomInternal(player, roomIndex);
    }

    private List<RunInfo> listRunsInternal() {
        List<RunInfo> result = new ArrayList<>();
        for (DungeonRun run : runsById.values()) {
            result.add(RunInfo.from(run));
        }
        result.sort(Comparator
            .comparing((RunInfo info) -> info.dungeonId())
            .thenComparing(RunInfo::runId));
        return result;
    }

    private Optional<RunInfo> findRunForPlayerInternal(UUID playerId) {
        for (DungeonRun run : runsById.values()) {
            if (run.hasMember(playerId)) {
                return Optional.of(RunInfo.from(run));
            }
        }
        return Optional.empty();
    }

    private boolean warpPlayerToRoomInternal(ServerPlayer player, int roomIndex) {
        MinecraftServer server = player.serverLevel().getServer();
        for (DungeonRun run : runsById.values()) {
            if (!run.hasMember(player.getUUID())) {
                continue;
            }
            run.teleportPlayerToRoom(server, player, roomIndex);
            return true;
        }
        return false;
    }

    public record RunInfo(UUID runId,
                          ResourceLocation dungeonId,
                          int currentRoomIndex,
                          String currentRoomId,
                          boolean waitingRoomClear,
                          boolean exhausted,
                          int alivePlayers,
                          int totalMembers) {

        private static RunInfo from(DungeonRun run) {
            return new RunInfo(
                run.runId(),
                run.dungeonId(),
                run.currentRoomIndex(),
                run.currentRoomId().orElse(null),
                run.isWaitingRoomClear(),
                run.isExhausted(),
                run.alivePlayers(),
                run.totalMembers());
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        INSTANCE.tick(event.getServer());
    }
}
