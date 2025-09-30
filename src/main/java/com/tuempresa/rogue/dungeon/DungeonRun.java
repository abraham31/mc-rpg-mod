package com.tuempresa.rogue.dungeon;

import com.tuempresa.rogue.RogueMod;
import com.tuempresa.rogue.config.RogueConfig;
import com.tuempresa.rogue.data.model.DungeonDef;
import com.tuempresa.rogue.data.model.MobEntry;
import com.tuempresa.rogue.data.model.RoomDef;
import com.tuempresa.rogue.data.model.WaveDef;
import com.tuempresa.rogue.world.RogueDimensions;
import com.tuempresa.rogue.world.TeleportUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/**
 * Encapsula el estado de una partida activa dentro de una mazmorra concreta.
 * Gestiona la sala actual, los temporizadores de las waves, la vida de los
 * mobs y el avance entre salas en función del progreso de los jugadores.
 */
final class DungeonRun {
    private static final String TAG_ROGUE_MOB = "rogue_mob";
    private static final String TAG_EARTH = "EARTH";
    private static final String TAG_RUN_ID = "RogueRunId";
    private static final String TAG_ROOM_INDEX = "RogueRoomIndex";

    private final DungeonInstance instance;
    private final DungeonDef dungeon;
    private final UUID runId;
    private final Set<UUID> alivePlayers = new HashSet<>();
    private final Set<UUID> activeMobs = new HashSet<>();
    private final Queue<ScheduledSpawn> pendingSpawns = new PriorityQueue<>(Comparator.comparingLong(ScheduledSpawn::triggerTick));
    private final RandomSource random = RandomSource.create();

    private int currentRoomIndex;
    private int currentWaveIndex;
    private RoomDef currentRoom;
    private WaveDef currentWave;
    private int warmupTimer;
    private boolean exhaustedContent;
    private boolean waitingRoomClear;
    private int roomClearTicks;
    private long tickCounter;

    DungeonRun(DungeonInstance instance, DungeonDef dungeon) {
        this.instance = Objects.requireNonNull(instance, "instance");
        this.dungeon = Objects.requireNonNull(dungeon, "dungeon");
        this.runId = UUID.randomUUID();
        this.currentRoomIndex = 0;
        this.currentWaveIndex = 0;
        this.warmupTimer = 0;
        this.tickCounter = 0L;
        this.waitingRoomClear = false;
        this.roomClearTicks = 0;
    }

    DungeonInstance instance() {
        return instance;
    }

    UUID runId() {
        return runId;
    }

    void join(ServerPlayer player) {
        instance.addPlayer(player);
        alivePlayers.add(player.getUUID());
    }

    TickResult tick(MinecraftServer server) {
        tickCounter++;
        refreshAlivePlayers(server);

        if (alivePlayers.isEmpty()) {
            if (RogueConfig.logRunLifecycle()) {
                RogueMod.LOGGER.debug("Finalizando mazmorra {}: no quedan jugadores vivos", dungeon.id());
            }
            return TickResult.defeat();
        }

        if (!exhaustedContent && !waitingRoomClear && currentWave == null) {
            prepareNextWave(server);
        }

        if (currentWave != null) {
            if (warmupTimer > 0) {
                warmupTimer--;
            }
            if (warmupTimer <= 0) {
                triggerWaveSpawn(server);
            }
        }

        processPendingSpawns(server);
        updateRoomProgress(server);

        if (isVictoryConditionMet()) {
            if (RogueConfig.logRunLifecycle()) {
                RogueMod.LOGGER.debug("Mazmorra {} completada", dungeon.id());
            }
            return TickResult.victory();
        }

        return TickResult.continueRun();
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

    private void prepareNextWave(MinecraftServer server) {
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
            waitingRoomClear = false;
            if (RogueConfig.logRoomLifecycle()) {
                RogueMod.LOGGER.debug("Iniciando sala {} de la mazmorra {}", currentRoom.id(), dungeon.id());
            }
            announceToPlayers(server, Component.literal("¡La sala " + currentRoom.id() + " ha comenzado!"));
        }

        if (waitingRoomClear) {
            return;
        }

        List<WaveDef> waves = currentRoom.waves();
        if (currentWaveIndex >= waves.size()) {
            waitingRoomClear = true;
            return;
        }

        currentWave = waves.get(currentWaveIndex++);
        warmupTimer = Math.max(0, currentWave.warmupTicks());
        if (RogueConfig.logSpawnLifecycle()) {
            RogueMod.LOGGER.debug("Preparando wave {} (warmup {} ticks) en sala {}", currentWave.index(), warmupTimer, currentRoom.id());
        }
    }

    private void triggerWaveSpawn(MinecraftServer server) {
        WaveDef wave = currentWave;
        if (wave == null) {
            return;
        }

        if (RogueConfig.logSpawnLifecycle()) {
            RogueMod.LOGGER.debug("Programando aparición de wave {} en la sala {} (mazmorra {})", wave.index(),
                currentRoom != null ? currentRoom.id() : "desconocida", dungeon.id());
        }

        for (MobEntry mob : wave.mobs()) {
            for (int i = 0; i < mob.count(); i++) {
                long triggerTick = tickCounter + Math.max(0, mob.spawnDelay());
                pendingSpawns.add(new ScheduledSpawn(currentRoomIndex, wave.index(), mob.entityType(), triggerTick));
            }
        }

        currentWave = null;
    }

    private void processPendingSpawns(MinecraftServer server) {
        while (!pendingSpawns.isEmpty() && pendingSpawns.peek().triggerTick <= tickCounter) {
            ScheduledSpawn spawn = pendingSpawns.poll();
            if (spawn == null) {
                continue;
            }

            spawnMob(server, spawn);
        }
    }

    private void spawnMob(MinecraftServer server, ScheduledSpawn spawn) {
        ServerLevel level = server.getLevel(RogueDimensions.EARTH_DUNGEON_LEVEL);
        if (level == null) {
            RogueMod.LOGGER.warn("No se encontró la dimensión de la mazmorra {} para spawnear mobs", dungeon.id());
            return;
        }

        Optional<EntityType<?>> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(spawn.entityType);
        if (entityType.isEmpty()) {
            RogueMod.LOGGER.warn("Entidad {} desconocida para la wave {} de la mazmorra {}", spawn.entityType, spawn.waveIndex, dungeon.id());
            return;
        }

        EntityType<?> rawType = entityType.get();
        Mob mob = rawType.create(level) instanceof Mob created ? created : null;
        if (mob == null) {
            RogueMod.LOGGER.warn("La entidad {} no es un Mob válido para la mazmorra {}", spawn.entityType, dungeon.id());
            return;
        }

        BlockPos anchor = roomAnchor(level, Math.max(0, spawn.roomIndex));
        BlockPos spawnPos = anchor.offset(random.nextIntBetweenInclusive(-2, 2), 0, random.nextIntBetweenInclusive(-2, 2));
        Vec3 target = Vec3.atCenterOf(spawnPos);

        DifficultyInstance difficulty = level.getCurrentDifficultyAt(spawnPos);
        mob.moveTo(target.x(), target.y(), target.z(), random.nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn((ServerLevelAccessor) level, difficulty, MobSpawnType.EVENT, null);
        mob.addTag(TAG_ROGUE_MOB);
        mob.addTag(TAG_EARTH);
        mob.setPersistenceRequired();
        mob.getPersistentData().putUUID(TAG_RUN_ID, runId);
        mob.getPersistentData().putInt(TAG_ROOM_INDEX, spawn.roomIndex);

        if (!level.tryAddFreshEntityWithPassengers(mob)) {
            mob.discard();
            RogueMod.LOGGER.warn("El nivel {} no aceptó al mob {} para la mazmorra {}", level.dimension().location(), spawn.entityType, dungeon.id());
            return;
        }

        onMobSpawned(mob.getUUID());
        if (RogueConfig.logSpawnLifecycle()) {
            RogueMod.LOGGER.debug("[{}] Spawned mob {} en la wave {} de la sala {}", dungeon.id(), spawn.entityType, spawn.waveIndex, spawn.roomIndex);
        }
    }

    private void updateRoomProgress(MinecraftServer server) {
        if (currentRoom == null) {
            roomClearTicks = 0;
            return;
        }

        if (!waitingRoomClear) {
            roomClearTicks = 0;
            return;
        }

        if (!pendingSpawns.isEmpty() || !activeMobs.isEmpty()) {
            roomClearTicks = 0;
            return;
        }

        roomClearTicks++;
        if (roomClearTicks < RogueConfig.roomClearThresholdTicks()) {
            return;
        }

        if (RogueConfig.logRoomLifecycle()) {
            RogueMod.LOGGER.debug("Sala {} despejada tras {} ticks", currentRoom.id(), roomClearTicks);
        }
        announceToPlayers(server, Component.literal("¡Has limpiado la sala " + currentRoom.id() + "!"));
        advanceRoom(server);
    }

    private void advanceRoom(MinecraftServer server) {
        currentRoom = null;
        currentWave = null;
        waitingRoomClear = false;
        roomClearTicks = 0;

        currentRoomIndex++;
        currentWaveIndex = 0;

        if (currentRoomIndex >= dungeon.rooms().size()) {
            exhaustedContent = true;
            announceToPlayers(server, Component.literal("¡Mazmorra completada!"));
            return;
        }

        teleportPartyToRoom(server, currentRoomIndex);
    }

    private void teleportPartyToRoom(MinecraftServer server, int roomIndex) {
        Optional<ServerLevel> targetLevel = TeleportUtil.level(server, RogueDimensions.EARTH_DUNGEON_LEVEL);
        if (targetLevel.isEmpty()) {
            return;
        }

        ServerLevel level = targetLevel.get();
        BlockPos target = roomAnchor(level, roomIndex);

        for (UUID memberId : instance.members()) {
            ServerPlayer player = server.getPlayerList().getPlayer(memberId);
            if (player == null) {
                continue;
            }
            TeleportUtil.teleport(player, RogueDimensions.EARTH_DUNGEON_LEVEL, target);
            player.sendSystemMessage(Component.literal("Avanzas a la sala " + roomIndex + "."));
        }
    }

    boolean ownsMob(UUID mobId) {
        return activeMobs.contains(mobId);
    }

    void onMobSpawned(UUID mobId) {
        activeMobs.add(mobId);
    }

    void onMobDefeated(UUID mobId) {
        activeMobs.remove(mobId);
    }

    void finish(MinecraftServer server, boolean victory) {
        cleanup(server);
        if (victory) {
            RewardSystem.grantVictoryRewards(server, this);
        } else {
            announceToPlayers(server, Component.literal("La mazmorra ha terminado. ¡Inténtalo de nuevo!"));
        }
    }

    void cleanup(MinecraftServer server) {
        Optional<ServerLevel> levelOpt = TeleportUtil.level(server, RogueDimensions.EARTH_DUNGEON_LEVEL);
        if (levelOpt.isPresent()) {
            ServerLevel level = levelOpt.get();
            List<UUID> mobs = new ArrayList<>(activeMobs);
            for (UUID mobId : mobs) {
                var entity = level.getEntity(mobId);
                if (entity instanceof Mob mob) {
                    mob.discard();
                }
            }
        }

        pendingSpawns.clear();
        activeMobs.clear();
        currentRoom = null;
        currentWave = null;
        waitingRoomClear = false;
        roomClearTicks = 0;
    }

    private boolean isVictoryConditionMet() {
        return exhaustedContent && pendingSpawns.isEmpty() && activeMobs.isEmpty() && currentWave == null;
    }

    ResourceLocation dungeonId() {
        return dungeon.id();
    }

    int currentRoomIndex() {
        return currentRoomIndex;
    }

    Optional<String> currentRoomId() {
        return Optional.ofNullable(currentRoom).map(RoomDef::id);
    }

    boolean isWaitingRoomClear() {
        return waitingRoomClear;
    }

    boolean isExhausted() {
        return exhaustedContent;
    }

    int alivePlayers() {
        return alivePlayers.size();
    }

    int totalMembers() {
        return instance.members().size();
    }

    boolean hasMember(UUID playerId) {
        return instance.members().contains(playerId);
    }

    void teleportPlayerToRoom(MinecraftServer server, ServerPlayer player, int roomIndex) {
        Optional<ServerLevel> targetLevel = TeleportUtil.level(server, RogueDimensions.EARTH_DUNGEON_LEVEL);
        if (targetLevel.isEmpty()) {
            return;
        }

        ServerLevel level = targetLevel.get();
        BlockPos target = roomAnchor(level, Math.max(0, roomIndex));
        TeleportUtil.teleport(player, RogueDimensions.EARTH_DUNGEON_LEVEL, target);
    }

    private void announceToPlayers(MinecraftServer server, Component message) {
        for (UUID memberId : instance.members()) {
            ServerPlayer player = server.getPlayerList().getPlayer(memberId);
            if (player != null) {
                player.sendSystemMessage(message);
            }
        }
    }

    private BlockPos roomAnchor(ServerLevel level, int roomIndex) {
        int spacing = RogueConfig.roomSpacingBlocks();
        BlockPos base = level.getSharedSpawnPos();
        int offsetX = roomIndex * spacing;
        return base.offset(offsetX, 0, 0);
    }

    record TickResult(boolean completed, boolean victory) {
        static TickResult continueRun() {
            return new TickResult(false, false);
        }

        static TickResult victory() {
            return new TickResult(true, true);
        }

        static TickResult defeat() {
            return new TickResult(true, false);
        }
    }

    private record ScheduledSpawn(int roomIndex, int waveIndex, ResourceLocation entityType, long triggerTick) {
    }
}
