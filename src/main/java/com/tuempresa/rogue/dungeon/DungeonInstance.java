package com.tuempresa.rogue.dungeon;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a running dungeon instance and keeps track of its party members.
 */
public final class DungeonInstance {
    private final ResourceLocation dungeonId;
    private final Set<UUID> members = Collections.newSetFromMap(new ConcurrentHashMap<>());

    DungeonInstance(ResourceLocation dungeonId) {
        this.dungeonId = dungeonId;
    }

    public ResourceLocation dungeonId() {
        return dungeonId;
    }

    public Set<UUID> members() {
        return Collections.unmodifiableSet(members);
    }

    void addPlayer(ServerPlayer player) {
        members.add(player.getUUID());
    }
}
