package com.tuempresa.rogue.dungeon;

import com.tuempresa.rogue.RogueMod;
import com.tuempresa.rogue.data.model.DungeonDef;
import com.tuempresa.rogue.data.model.MobEntry;
import com.tuempresa.rogue.data.model.RoomDef;
import com.tuempresa.rogue.data.model.WaveDef;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/**
 * Encapsula el estado de una partida activa dentro de una mazmorra concreta.
 * Gestiona la sala actual, los temporizadores de las waves y los jugadores
 * vivos para que {@link DungeonManager} pueda avanzar la partida en cada tick
 * del servidor.
 */
final class DungeonRun {
    private final DungeonInstance instance;
    private final DungeonDef dungeon;
    private final Set<UUID> alivePlayers = new HashSet<>();
    private final Queue<ScheduledSpawn> pendingSpawns = new PriorityQueue<>(Comparator.comparingLong(ScheduledSpawn::triggerTick));

    private int currentRoomIndex;
    private int currentWaveIndex;
    private RoomDef currentRoom;
    private WaveDef currentWave;
    private int warmupTimer;
    private boolean exhaustedContent;
    private long tickCounter;

    DungeonRun(DungeonInstance instance, DungeonDef dungeon) {
        this.instance = Objects.requireNonNull(instance, "instance");
        this.dungeon = Objects.requireNonNull(dungeon, "dungeon");
        this.currentRoomIndex = 0;
        this.currentWaveIndex = 0;
        this.warmupTimer = 0;
        this.tickCounter = 0L;
    }

    DungeonInstance instance() {
        return instance;
    }

    void join(ServerPlayer player) {
        instance.addPlayer(player);
        alivePlayers.add(player.getUUID());
    }

    boolean isComplete() {
        return exhaustedContent && pendingSpawns.isEmpty() && currentWave == null;
    }

    boolean tick(MinecraftServer server) {
        tickCounter++;
        refreshAlivePlayers(server);
        if (alivePlayers.isEmpty()) {
            RogueMod.LOGGER.debug("Finalizando mazmorra {}: no quedan jugadores vivos", dungeon.id());
            return true;
        }

        if (!exhaustedContent && currentWave == null) {
            prepareNextWave();
        }

        if (currentWave != null) {
            if (warmupTimer > 0) {
                warmupTimer--;
            }
            if (warmupTimer <= 0) {
                triggerWaveSpawn();
            }
        }

        processPendingSpawns();
        return isComplete();
    }

    private void refreshAlivePlayers(MinecraftServer server) {
        Set<UUID> currentlyAlive = new HashSet<>();
        for (UUID memberId : instance.members()) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member != null && member.isAlive()) {
                currentlyAlive.add(memberId);
            }
        }

        alivePlayers.clear();
        alivePlayers.addAll(currentlyAlive);
    }

    private void prepareNextWave() {
        if (exhaustedContent) {
            return;
        }

        if (currentRoom == null) {
            if (currentRoomIndex >= dungeon.rooms().size()) {
                exhaustedContent = true;
                return;
            }

            currentRoom = dungeon.rooms().get(currentRoomIndex);
            currentWaveIndex = 0;
            RogueMod.LOGGER.debug("Iniciando sala {} de la mazmorra {}", currentRoom.id(), dungeon.id());
        }

        if (currentWaveIndex >= currentRoom.waves().size()) {
            currentRoomIndex++;
            currentRoom = null;
            prepareNextWave();
            return;
        }

        currentWave = currentRoom.waves().get(currentWaveIndex++);
        warmupTimer = currentWave.warmupTicks();
        RogueMod.LOGGER.debug("Preparando wave {} (warmup {} ticks) en sala {}", currentWave.index(), warmupTimer,
            currentRoom.id());
    }

    private void triggerWaveSpawn() {
        WaveDef wave = currentWave;
        if (wave == null) {
            return;
        }

        RogueMod.LOGGER.debug("Programando aparición de wave {} en la sala {} (mazmorra {})", wave.index(),
            currentRoom != null ? currentRoom.id() : "desconocida", dungeon.id());

        for (MobEntry mob : wave.mobs()) {
            for (int i = 0; i < mob.count(); i++) {
                long triggerTick = tickCounter + mob.spawnDelay();
                pendingSpawns.add(new ScheduledSpawn(mob.entityType(), triggerTick, wave.index()));
            }
        }

        currentWave = null;
    }

    private void processPendingSpawns() {
        while (!pendingSpawns.isEmpty() && pendingSpawns.peek().triggerTick() <= tickCounter) {
            ScheduledSpawn spawn = pendingSpawns.poll();
            if (spawn == null) {
                continue;
            }

            RogueMod.LOGGER.debug(
                "[{}] Spawn ficticio del mob {} en la wave {}",
                dungeon.id(),
                spawn.entityType(),
                spawn.waveIndex());
        }

        if (pendingSpawns.isEmpty() && exhaustedContent && currentWave == null) {
            RogueMod.LOGGER.debug("Mazmorra {} completada", dungeon.id());
        }
    }

    private record ScheduledSpawn(ResourceLocation entityType, long triggerTick, int waveIndex) {
    }
}
