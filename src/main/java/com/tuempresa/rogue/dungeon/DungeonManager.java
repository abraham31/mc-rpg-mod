package com.tuempresa.rogue.dungeon;

import com.tuempresa.rogue.data.model.PortalDef;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps track of active dungeon instances and ensures players are grouped in
 * a deterministic fashion when entering through a portal.
 */
public final class DungeonManager {
    private static final DungeonManager INSTANCE = new DungeonManager();

    private final Map<ResourceLocation, DungeonInstance> activeInstances = new ConcurrentHashMap<>();

    private DungeonManager() {
    }

    public static DungeonInstance createOrJoin(ServerPlayer player, PortalDef portal) {
        return INSTANCE.createOrJoinInternal(player, portal);
    }

    private DungeonInstance createOrJoinInternal(ServerPlayer player, PortalDef portal) {
        ResourceLocation dungeonId = portal.dungeonId();
        DungeonInstance instance = activeInstances.computeIfAbsent(dungeonId, DungeonInstance::new);
        instance.addPlayer(player);
        return instance;
    }
}
