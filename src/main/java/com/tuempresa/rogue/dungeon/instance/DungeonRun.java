package com.tuempresa.rogue.dungeon.instance;

import com.tuempresa.rogue.config.RogueConfig;
import com.tuempresa.rogue.core.RogueBlocks;
import com.tuempresa.rogue.core.RogueConstants;
import com.tuempresa.rogue.data.model.DungeonDef;
import com.tuempresa.rogue.data.model.WaveDef;
import com.tuempresa.rogue.dungeon.room.RoomState;
import com.tuempresa.rogue.reward.RewardSystem;
import com.tuempresa.rogue.reward.awakening.ArmorAwakening;
import com.tuempresa.rogue.reward.awakening.WeaponAwakening;
import com.tuempresa.rogue.spawn.SpawnSystem;
import com.tuempresa.rogue.util.RogueLogger;
import com.tuempresa.rogue.util.TP;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DungeonRun {
    private final UUID id = UUID.randomUUID();
    private final DungeonDef def;
    private final List<RoomState> rooms = new ArrayList<>();
    private final Set<UUID> party = new LinkedHashSet<>();
    private final Set<UUID> spawnedMobs = new LinkedHashSet<>();
    private final Map<UUID, Integer> mobRooms = new HashMap<>();
    private int currentRoomIndex;
    private int ticksInRoom;
    private boolean finished;
    private boolean victory;
    private final Map<UUID, Integer> awakenings = new HashMap<>();

    public DungeonRun(DungeonDef def) {
        this.def = def;
        def.rooms().forEach(room -> rooms.add(new RoomState(room)));
    }

    public UUID getId() {
        return id;
    }

    public DungeonDef getDef() {
        return def;
    }

    public Set<UUID> getParty() {
        return party;
    }

    public RoomState getCurrentRoom() {
        if (rooms.isEmpty()) {
            return null;
        }
        if (currentRoomIndex >= rooms.size()) {
            return rooms.get(rooms.size() - 1);
        }
        return rooms.get(currentRoomIndex);
    }

    public RoomState findRoomById(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            return null;
        }
        for (RoomState room : rooms) {
            if (roomId.equals(room.getDef().id())) {
                return room;
            }
        }
        return null;
    }

    public void addMember(ServerPlayer player) {
        party.add(player.getUUID());
    }

    public void tick(MinecraftServer server) {
        if (finished) {
            return;
        }
        applyArmorBonuses(server);
        ticksInRoom++;
        if (ticksInRoom >= RogueConfig.roomTimeLimitTicks()) {
            finishFail(server);
            return;
        }

        RoomState room = getCurrentRoom();
        if (room == null) {
            finishFail(server);
            return;
        }

        if (room.isCleared() && room.isStarted()) {
            advanceRoom(server);
            return;
        }

        WaveDef wave = room.getDef().wave();
        if (wave != null && room.isWaveReady(ticksInRoom) && room.getAlive() < wave.maxAlive()) {
            spawnWave(server);
        }
    }

    public void spawnWave(MinecraftServer server) {
        if (finished) {
            return;
        }
        RoomState room = getCurrentRoom();
        if (room == null || room.isCleared()) {
            return;
        }

        WaveDef wave = room.getDef().wave();
        if (wave == null) {
            return;
        }
        if (room.getAlive() >= wave.maxAlive()) {
            return;
        }

        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, def.world()));
        if (level == null) {
            RogueLogger.warn("No se pudo cargar el nivel {} para run {}", def.world(), id);
            return;
        }

        SpawnSystem.spawnWave(level, this, room, currentRoomIndex);
        room.scheduleNextWave(ticksInRoom + wave.cooldownTicks());
    }

    public int countAlive() {
        RoomState room = getCurrentRoom();
        return room != null ? room.getAlive() : 0;
    }

    private void advanceRoom(MinecraftServer server) {
        RoomState room = getCurrentRoom();
        if (room == null || !room.isCleared()) {
            return;
        }

        if (currentRoomIndex >= rooms.size() - 1) {
            finishSuccess(server);
            return;
        }

        ticksInRoom = 0;
        currentRoomIndex++;
        RoomState next = rooms.get(currentRoomIndex);
        next.resetState(0);
        spawnWave(server);
    }

    public void finishSuccess(MinecraftServer server) {
        if (finished) {
            return;
        }
        finished = true;
        victory = true;
        RewardSystem.grantRewards(server, this);
        openExitPortal(server);
        cleanupEntities(server);
        resetAwakenings(server);
        returnAllToCity(server);
    }

    public void finishFail(MinecraftServer server) {
        if (finished) {
            return;
        }
        finished = true;
        victory = false;
        cleanupEntities(server);
        resetAwakenings(server);
        returnAllToCity(server);
    }

    public void cleanup(MinecraftServer server) {
        cleanupEntities(server);
        resetAwakenings(server);
        returnAllToCity(server);
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isVictory() {
        return victory;
    }

    public void openExitPortal(MinecraftServer server) {
        RogueLogger.info("Run {} completada, generando portal de salida.", id);
        RoomState lastRoom = rooms.isEmpty() ? null : rooms.get(rooms.size() - 1);
        if (lastRoom == null) {
            return;
        }
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, def.world()));
        if (level == null) {
            RogueLogger.warn("No se pudo generar el portal de salida en {}", def.world());
            return;
        }

        BlockPos floorPos = new BlockPos(
            (int) Math.floor(lastRoom.getBounds().getCenter().x),
            (int) Math.floor(lastRoom.getBounds().minY),
            (int) Math.floor(lastRoom.getBounds().getCenter().z)
        );
        BlockPos portalPos = floorPos.above();

        BlockState baseState = Blocks.SMOOTH_STONE.defaultBlockState();
        if (!level.getBlockState(floorPos).is(baseState.getBlock())) {
            level.setBlockAndUpdate(floorPos, baseState);
        }

        BlockState portalState = RogueBlocks.PORTAL_TIERRA.get().defaultBlockState();
        level.setBlockAndUpdate(portalPos, portalState);
    }

    public void returnToCity(ServerPlayer player) {
        TP.toSpawn(player, RogueConstants.DIM_CITY1);
    }

    private void returnAllToCity(MinecraftServer server) {
        party.forEach(uuid -> {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                returnToCity(player);
            }
        });
    }

    public void registerMob(UUID mobId, int roomIndex) {
        spawnedMobs.add(mobId);
        mobRooms.put(mobId, roomIndex);
        rooms.get(roomIndex).registerMob(mobId);
    }

    public void onMobKilled(MinecraftServer server, UUID mobId) {
        Integer roomIndex = mobRooms.remove(mobId);
        if (roomIndex == null) {
            return;
        }
        spawnedMobs.remove(mobId);
        if (roomIndex >= 0 && roomIndex < rooms.size()) {
            RoomState room = rooms.get(roomIndex);
            room.unregisterMob(mobId);
            if (!finished && roomIndex == currentRoomIndex && room.isCleared()) {
                advanceRoom(server);
            }
        }
    }

    public boolean hasMember(UUID playerId) {
        return party.contains(playerId);
    }

    public Set<UUID> getSpawnedMobs() {
        return spawnedMobs;
    }

    public void clearTracked() {
        spawnedMobs.clear();
        mobRooms.clear();
        rooms.forEach(RoomState::clearTracked);
    }

    public boolean grantAwakening(ServerPlayer player) {
        UUID uuid = player.getUUID();
        int current = awakenings.getOrDefault(uuid, 0);
        if (current >= 3) {
            return false;
        }
        awakenings.put(uuid, current + 1);
        return true;
    }

    private void resetAwakenings(MinecraftServer server) {
        awakenings.clear();
        party.forEach(uuid -> {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                ArmorAwakening.reset(player);
                WeaponAwakening.reset(player);
            }
        });
    }

    private void cleanupEntities(MinecraftServer server) {
        ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, def.world());
        ServerLevel level = server.getLevel(levelKey);
        if (level == null) {
            return;
        }
        SpawnSystem.cleanup(level, spawnedMobs);
        clearTracked();
    }

    private void applyArmorBonuses(MinecraftServer server) {
        for (UUID uuid : party) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) {
                continue;
            }
            if (!isWearingReinforcedArmor(player)) {
                continue;
            }
            int level = ArmorAwakening.getLevel(player);
            if (level < 1 || level > 2) {
                continue;
            }
            int amplifier = Math.max(0, level - 1);
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 60, amplifier, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, amplifier, true, false, true));
        }
    }

    private static boolean isWearingReinforcedArmor(ServerPlayer player) {
        for (ItemStack armorPiece : player.getInventory().armor) {
            if (!armorPiece.isEmpty() && armorPiece.is(RogueConstants.TAG_ARMADURAS_REFORZADAS)) {
                return true;
            }
        }
        return false;
    }
}
