package com.tuempresa.rogue.dungeon;

import com.tuempresa.rogue.RogueMod;
import com.tuempresa.rogue.data.model.DungeonDef;
import com.tuempresa.rogue.data.model.PortalDef;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Iterator;
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
                RogueMod.LOGGER.debug("Finalizada la instancia de la mazmorra {}", entry.getKey());
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

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        INSTANCE.tick(event.getServer());
    }
}
