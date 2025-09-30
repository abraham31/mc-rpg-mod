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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps track of active dungeon runs and ensures players are grouped in a
 * deterministic fashion when entering through a portal.
 */
@Mod.EventBusSubscriber(modid = RogueMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DungeonManager {
    private static final DungeonManager INSTANCE = new DungeonManager();

    private final Map<ResourceLocation, DungeonRun> activeRuns = new ConcurrentHashMap<>();

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
            if (current == null || current.isComplete()) {
                current = new DungeonRun(new DungeonInstance(id), dungeonDef);
            }
            current.join(player);
            return current;
        });

        if (run == null) {
            throw new IllegalStateException("No se pudo crear la instancia para la mazmorra " + dungeonId);
        }

        return run.instance();
    }

    private void tick(MinecraftServer server) {
        Iterator<Map.Entry<ResourceLocation, DungeonRun>> iterator = activeRuns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ResourceLocation, DungeonRun> entry = iterator.next();
            DungeonRun run = entry.getValue();
            if (run.tick(server)) {
                RogueMod.LOGGER.debug("Finalizada la instancia de la mazmorra {}", entry.getKey());
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        INSTANCE.tick(event.getServer());
    }
}
