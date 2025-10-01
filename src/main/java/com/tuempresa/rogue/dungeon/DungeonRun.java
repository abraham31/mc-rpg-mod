package com.tuempresa.rogue.dungeon;

import com.tuempresa.rogue.RogueMod;
import com.tuempresa.rogue.config.RogueConfig;
import com.tuempresa.rogue.data.model.DungeonDef;
import com.tuempresa.rogue.data.model.MobEntry;
import com.tuempresa.rogue.data.model.RoomDef;
import com.tuempresa.rogue.data.model.WaveDef;
import com.tuempresa.rogue.dungeon.spawn.SpawnSystem;
import com.tuempresa.rogue.world.RogueDimensions;
import com.tuempresa.rogue.world.TeleportUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

    private static final int DUNGEON_FLOOR_Y = 64;
    private static final int ROOM_HALF_WIDTH = 6;
    private static final int ROOM_HALF_LENGTH = 6;
    private static final int ROOM_HEIGHT = 6;
    private static final long SPAWN_RETRY_DELAY_TICKS = 20L;
    private static final long EXIT_PORTAL_TIMEOUT_TICKS = 20L * 30L;
    private static final long EXIT_PORTAL_TELEPORT_DELAY_TICKS = 20L * 5L;
    private static final ResourceLocation EXIT_PORTAL_BLOCK_ID = RogueMod.id("city1_portal");
    private static final int[] ROOM_TIME_WARNING_SECONDS = {10, 5, 3, 1};

    private final DungeonInstance instance;
    private final DungeonDef dungeon;
    private final UUID runId;
    private final Set<UUID> alivePlayers = new HashSet<>();
    private final Map<UUID, Integer> activeMobs = new HashMap<>();
    private final Queue<ScheduledSpawn> pendingSpawns = new PriorityQueue<>(Comparator.comparingLong(ScheduledSpawn::triggerTick));
    private final RandomSource random = RandomSource.create();
    private final Set<Integer> builtRooms = new HashSet<>();
    private final Map<Integer, BoundingBox> roomBoundsCache = new HashMap<>();
    private final Set<Integer> issuedTimeWarnings = new HashSet<>();

    private int currentRoomIndex;
    private int currentWaveIndex;
    private RoomDef currentRoom;
    private WaveDef currentWave;
    private int warmupTimer;
    private boolean exhaustedContent;
    private boolean waitingRoomClear;
    private int roomClearTicks;
    private long tickCounter;
    private int roomTimeRemaining;
    private boolean finished;
    private BlockPos exitPortalPos;
    private Block exitPortalBlock;
    private long exitPortalTimeoutToken;

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
        this.roomTimeRemaining = 0;
        this.finished = false;
        this.exitPortalPos = null;
        this.exitPortalBlock = null;
        this.exitPortalTimeoutToken = 0L;
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
        if (finished) {
            return TickResult.defeat();
        }

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
        enforceMobBounds(server);
        updateRoomProgress(server);

        if (tickRoomTimer(server)) {
            return TickResult.defeat();
        }

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
            roomTimeRemaining = Math.max(0, currentRoom.timeLimitTicks());
            issuedTimeWarnings.clear();
            if (RogueConfig.logRoomLifecycle()) {
                RogueMod.LOGGER.debug("Iniciando sala {} de la mazmorra {}", currentRoom.id(), dungeon.id());
            }
            announceToPlayers(server, Component.literal("¡La sala " + currentRoom.id() + " ha comenzado!"));
            if (roomTimeRemaining > 0) {
                int seconds = (roomTimeRemaining + 19) / 20;
                announceToPlayers(server, Component.literal("Tienes " + seconds + " segundos para completar la sala."));
            }
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
        while (!pendingSpawns.isEmpty()) {
            ScheduledSpawn next = pendingSpawns.peek();
            if (next == null) {
                pendingSpawns.poll();
                continue;
            }

            if (next.triggerTick > tickCounter) {
                break;
            }

            int maxAlive = roomMaxAlive(next.roomIndex());
            int aliveInRoom = activeMobCountForRoom(next.roomIndex());
            if (aliveInRoom >= maxAlive) {
                pendingSpawns.poll();
                long retryTick = Math.max(tickCounter + SPAWN_RETRY_DELAY_TICKS, next.triggerTick + SPAWN_RETRY_DELAY_TICKS);
                pendingSpawns.add(next.delayUntil(retryTick));
                continue;
            }

            ScheduledSpawn spawn = pendingSpawns.poll();
            spawnMob(server, spawn);
        }
    }

    private int activeMobCountForRoom(int roomIndex) {
        int count = 0;
        for (Integer mobRoom : activeMobs.values()) {
            if (mobRoom != null && mobRoom == roomIndex) {
                count++;
            }
        }
        return count;
    }

    private int roomMaxAlive(int roomIndex) {
        if (roomIndex >= 0 && roomIndex < dungeon.rooms().size()) {
            return Math.max(1, dungeon.rooms().get(roomIndex).maxAlive());
        }
        return Integer.MAX_VALUE;
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

        BlockPos anchor = roomAnchor(level, Math.max(0, spawn.roomIndex));
        ensureRoomBuilt(level, Math.max(0, spawn.roomIndex));
        BoundingBox bounds = roomBounds(level, Math.max(0, spawn.roomIndex));
        Optional<BlockPos> spawnPos = bounds != null
            ? SpawnSystem.findSpawnPosition(level, bounds, rawType, random)
            : Optional.empty();
        BlockPos targetPos = spawnPos.orElse(anchor);

        Optional<Mob> createdMob = SpawnSystem.createMob(level, targetPos, rawType, random);
        if (createdMob.isEmpty()) {
            RogueMod.LOGGER.warn("No se pudo crear la entidad {} para la mazmorra {}", spawn.entityType, dungeon.id());
            return;
        }

        Mob mob = createdMob.get();
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

        onMobSpawned(mob.getUUID(), spawn.roomIndex);
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
        roomTimeRemaining = 0;
        issuedTimeWarnings.clear();

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
        ensureRoomBuilt(level, roomIndex);
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
        return activeMobs.containsKey(mobId);
    }

    void onMobSpawned(UUID mobId, int roomIndex) {
        activeMobs.put(mobId, roomIndex);
    }

    void onMobDefeated(UUID mobId) {
        activeMobs.remove(mobId);
    }

    void finish(MinecraftServer server, boolean victory) {
        if (finished) {
            return;
        }
        finished = true;
        if (victory) {
            handleVictoryFinish(server);
            return;
        }

        teleportPartyToCity(server, Component.literal("Has sido devuelto a Ciudad1 sin recompensa"));
        cleanup(server);
    }

    private void handleVictoryFinish(MinecraftServer server) {
        placeExitPortal(server);
        long delay = Math.max(0L, EXIT_PORTAL_TELEPORT_DELAY_TICKS);
        if (delay <= 0L) {
            finalizeVictory(server);
            return;
        }
        scheduleTask(server, delay, () -> finalizeVictory(server));
    }

    private void finalizeVictory(MinecraftServer server) {
        if (!finished) {
            return;
        }
        RewardSystem.grantVictoryRewards(server, this);
        teleportPartyToCity(server, null);
        cleanup(server);
    }

    private void teleportPartyToCity(MinecraftServer server, Component postTeleportMessage) {
        Optional<ServerLevel> targetLevel = TeleportUtil.level(server, RogueDimensions.CITY1_LEVEL);
        if (targetLevel.isEmpty()) {
            return;
        }

        ServerLevel city = targetLevel.get();
        BlockPos spawn = city.getSharedSpawnPos();
        for (UUID memberId : instance.members()) {
            ServerPlayer player = server.getPlayerList().getPlayer(memberId);
            if (player == null) {
                continue;
            }
            TeleportUtil.teleport(player, RogueDimensions.CITY1_LEVEL, spawn);
            if (postTeleportMessage != null) {
                player.sendSystemMessage(postTeleportMessage);
            }
        }
    }

    void cleanup(MinecraftServer server) {
        Optional<ServerLevel> levelOpt = TeleportUtil.level(server, RogueDimensions.EARTH_DUNGEON_LEVEL);
        if (levelOpt.isPresent()) {
            ServerLevel level = levelOpt.get();
            List<UUID> mobs = new ArrayList<>(activeMobs.keySet());
            for (UUID mobId : mobs) {
                var entity = level.getEntity(mobId);
                if (entity instanceof Mob mob) {
                    mob.discard();
                }
            }
            removeExitPortal(level);
        }
        if (levelOpt.isEmpty()) {
            exitPortalPos = null;
            exitPortalBlock = null;
            exitPortalTimeoutToken++;
        }

        pendingSpawns.clear();
        activeMobs.clear();
        currentRoom = null;
        currentWave = null;
        waitingRoomClear = false;
        roomClearTicks = 0;
        roomTimeRemaining = 0;
        issuedTimeWarnings.clear();
    }

    private boolean isVictoryConditionMet() {
        return exhaustedContent && pendingSpawns.isEmpty() && activeMobs.isEmpty() && currentWave == null;
    }

    private boolean tickRoomTimer(MinecraftServer server) {
        if (currentRoom == null) {
            return false;
        }

        if (roomTimeRemaining <= 0) {
            return false;
        }

        roomTimeRemaining--;

        if (roomTimeRemaining <= 0) {
            if (RogueConfig.logRunLifecycle()) {
                RogueMod.LOGGER.debug("Finalizando mazmorra {}: se agotó el tiempo en la sala {}", dungeon.id(), currentRoom.id());
            }
            announceToPlayers(server, Component.literal("¡Se acabó el tiempo para la sala " + currentRoom.id() + "!"));
            finish(server, false);
            return true;
        }

        int secondsRemaining = Math.max(0, (roomTimeRemaining + 19) / 20);
        for (int threshold : ROOM_TIME_WARNING_SECONDS) {
            if (secondsRemaining <= threshold && issuedTimeWarnings.add(threshold)) {
                announceToPlayers(server, Component.literal("Quedan " + secondsRemaining + " segundos para completar la sala."));
                break;
            }
        }

        return false;
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
        ensureRoomBuilt(level, Math.max(0, roomIndex));
        BlockPos target = roomAnchor(level, Math.max(0, roomIndex));
        TeleportUtil.teleport(player, RogueDimensions.EARTH_DUNGEON_LEVEL, target);
    }

    private void placeExitPortal(MinecraftServer server) {
        if (dungeon.rooms().isEmpty()) {
            return;
        }

        Optional<ServerLevel> levelOpt = TeleportUtil.level(server, RogueDimensions.EARTH_DUNGEON_LEVEL);
        if (levelOpt.isEmpty()) {
            return;
        }

        ServerLevel level = levelOpt.get();
        int finalRoomIndex = currentRoom != null
            ? Math.max(0, Math.min(dungeon.rooms().size() - 1, currentRoomIndex))
            : Math.max(0, Math.min(dungeon.rooms().size() - 1, currentRoomIndex - 1));
        ensureRoomBuilt(level, finalRoomIndex);
        BlockPos anchor = roomAnchor(level, finalRoomIndex);

        removeExitPortal(level);

        BlockState state = exitPortalState();
        if (!level.setBlock(anchor, state, 3)) {
            exitPortalPos = null;
            exitPortalBlock = null;
            exitPortalTimeoutToken++;
            return;
        }
        exitPortalPos = anchor.immutable();
        exitPortalBlock = state.getBlock();
        long token = ++exitPortalTimeoutToken;
        announceToPlayers(server, Component.literal("Portal de regreso abierto"));
        if (EXIT_PORTAL_TIMEOUT_TICKS > 0L) {
            schedulePortalTimeout(server, token, EXIT_PORTAL_TIMEOUT_TICKS);
        }
    }

    private BlockState exitPortalState() {
        return BuiltInRegistries.BLOCK.getOptional(EXIT_PORTAL_BLOCK_ID)
            .map(Block::defaultBlockState)
            .orElse(Blocks.NETHER_PORTAL.defaultBlockState());
    }

    private void schedulePortalTimeout(MinecraftServer server, long token, long remainingTicks) {
        if (remainingTicks <= 0L) {
            TeleportUtil.level(server, RogueDimensions.EARTH_DUNGEON_LEVEL)
                .ifPresent(this::removeExitPortal);
            return;
        }

        server.execute(() -> continuePortalTimeout(server, token, remainingTicks - 1L));
    }

    private void continuePortalTimeout(MinecraftServer server, long token, long remainingTicks) {
        if (exitPortalPos == null || exitPortalTimeoutToken != token) {
            return;
        }

        if (remainingTicks <= 0L) {
            TeleportUtil.level(server, RogueDimensions.EARTH_DUNGEON_LEVEL)
                .ifPresent(this::removeExitPortal);
            return;
        }

        server.execute(() -> continuePortalTimeout(server, token, remainingTicks - 1L));
    }

    private void removeExitPortal(ServerLevel level) {
        if (exitPortalPos == null) {
            return;
        }

        if (exitPortalBlock != null && !level.getBlockState(exitPortalPos).is(exitPortalBlock)) {
            exitPortalPos = null;
            exitPortalBlock = null;
            exitPortalTimeoutToken++;
            return;
        }

        level.setBlock(exitPortalPos, Blocks.AIR.defaultBlockState(), 3);
        exitPortalPos = null;
        exitPortalBlock = null;
        exitPortalTimeoutToken++;
    }

    private void scheduleTask(MinecraftServer server, long delayTicks, Runnable action) {
        if (delayTicks <= 0L) {
            action.run();
            return;
        }

        server.execute(() -> scheduleTask(server, delayTicks - 1L, action));
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
        BlockPos adjustedBase = new BlockPos(base.getX(), DUNGEON_FLOOR_Y + 1, base.getZ());
        int offsetX = roomIndex * spacing;
        return adjustedBase.offset(offsetX, 0, 0);
    }

    private void ensureRoomBuilt(ServerLevel level, int roomIndex) {
        BlockPos anchor = roomAnchor(level, roomIndex);
        int floorY = DUNGEON_FLOOR_Y;
        int minX = anchor.getX() - ROOM_HALF_WIDTH;
        int maxX = anchor.getX() + ROOM_HALF_WIDTH;
        int minZ = anchor.getZ() - ROOM_HALF_LENGTH;
        int maxZ = anchor.getZ() + ROOM_HALF_LENGTH;
        int ceilingY = floorY + ROOM_HEIGHT;

        int interiorMinX = minX + 1;
        int interiorMaxX = maxX - 1;
        int interiorMinZ = minZ + 1;
        int interiorMaxZ = maxZ - 1;
        int interiorMinY = floorY + 1;
        int interiorMaxY = ceilingY - 1;

        BoundingBox bounds = new BoundingBox(
            interiorMinX,
            interiorMinY,
            interiorMinZ,
            interiorMaxX,
            Math.max(interiorMinY, interiorMaxY),
            interiorMaxZ);
        roomBoundsCache.put(roomIndex, bounds);

        if (!builtRooms.add(roomIndex)) {
            return;
        }

        if (roomIndex == 0) {
            level.setDefaultSpawnPos(new BlockPos(anchor.getX(), DUNGEON_FLOOR_Y + 1, anchor.getZ()), 0.0F);
        }

        BlockState floorBlock = Blocks.SMOOTH_STONE.defaultBlockState();
        BlockState wallBlock = Blocks.STONE_BRICKS.defaultBlockState();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                cursor.set(x, floorY, z);
                level.setBlock(cursor, floorBlock, 3);
                for (int y = level.getMinBuildHeight(); y < floorY; y++) {
                    cursor.set(x, y, z);
                    level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        for (int x = interiorMinX; x <= interiorMaxX; x++) {
            for (int z = interiorMinZ; z <= interiorMaxZ; z++) {
                for (int y = interiorMinY; y <= interiorMaxY; y++) {
                    cursor.set(x, y, z);
                    level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        for (int x = minX; x <= maxX; x++) {
            for (int y = floorY + 1; y <= ceilingY; y++) {
                cursor.set(x, y, minZ);
                level.setBlock(cursor, wallBlock, 3);
                cursor.set(x, y, maxZ);
                level.setBlock(cursor, wallBlock, 3);
            }
        }

        for (int z = minZ; z <= maxZ; z++) {
            for (int y = floorY + 1; y <= ceilingY; y++) {
                cursor.set(minX, y, z);
                level.setBlock(cursor, wallBlock, 3);
                cursor.set(maxX, y, z);
                level.setBlock(cursor, wallBlock, 3);
            }
        }

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                cursor.set(x, ceilingY, z);
                level.setBlock(cursor, wallBlock, 3);
            }
        }
    }

    private BoundingBox roomBounds(ServerLevel level, int roomIndex) {
        BoundingBox bounds = roomBoundsCache.get(roomIndex);
        if (bounds == null) {
            ensureRoomBuilt(level, roomIndex);
            bounds = roomBoundsCache.get(roomIndex);
        }
        if (bounds == null) {
            BlockPos anchor = roomAnchor(level, roomIndex);
            int floorY = DUNGEON_FLOOR_Y;
            int minX = anchor.getX() - ROOM_HALF_WIDTH + 1;
            int maxX = anchor.getX() + ROOM_HALF_WIDTH - 1;
            int minZ = anchor.getZ() - ROOM_HALF_LENGTH + 1;
            int maxZ = anchor.getZ() + ROOM_HALF_LENGTH - 1;
            int minY = floorY + 1;
            int maxY = floorY + ROOM_HEIGHT - 1;
            if (minX > maxX) {
                minX = maxX = anchor.getX();
            }
            if (minZ > maxZ) {
                minZ = maxZ = anchor.getZ();
            }
            if (minY > maxY) {
                minY = maxY = floorY + 1;
            }
            bounds = new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
            roomBoundsCache.put(roomIndex, bounds);
        }
        return bounds;
    }

    private void enforceMobBounds(MinecraftServer server) {
        Optional<ServerLevel> levelOpt = TeleportUtil.level(server, RogueDimensions.EARTH_DUNGEON_LEVEL);
        if (levelOpt.isEmpty()) {
            return;
        }

        ServerLevel level = levelOpt.get();
        Iterator<Map.Entry<UUID, Integer>> iterator = activeMobs.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            UUID mobId = entry.getKey();
            var entity = level.getEntity(mobId);
            if (!(entity instanceof Mob mob)) {
                iterator.remove();
                continue;
            }

            int roomIndex = entry.getValue() != null ? entry.getValue() : -1;
            if (mob.getPersistentData().contains(TAG_ROOM_INDEX)) {
                roomIndex = mob.getPersistentData().getInt(TAG_ROOM_INDEX);
                entry.setValue(roomIndex);
            }
            if (roomIndex < 0) {
                continue;
            }
            BoundingBox bounds = roomBounds(level, Math.max(0, roomIndex));
            BlockPos pos = mob.blockPosition();
            boolean outOfBounds = bounds == null || !bounds.isInside(pos);
            boolean invalidGround = !hasSolidGround(level, pos);
            if (outOfBounds || invalidGround) {
                BlockPos anchor = roomAnchor(level, Math.max(0, roomIndex));
                mob.teleportTo(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D);
                mob.setDeltaMovement(Vec3.ZERO);
                mob.fallDistance = 0.0F;
                mob.getNavigation().stop();
            }
        }
    }

    private boolean hasSolidGround(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState ground = level.getBlockState(below);
        return !ground.isAir() && ground.isFaceSturdy(level, below, Direction.UP);
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
        ScheduledSpawn delayUntil(long newTick) {
            return new ScheduledSpawn(roomIndex, waveIndex, entityType, newTick);
        }
    }
}
