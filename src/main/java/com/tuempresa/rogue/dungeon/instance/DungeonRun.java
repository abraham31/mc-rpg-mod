package com.tuempresa.rogue.dungeon.instance;

import com.tuempresa.rogue.config.RogueConfig;
import com.tuempresa.rogue.core.RogueConstants;
import com.tuempresa.rogue.data.model.DungeonDef;
import com.tuempresa.rogue.dungeon.room.RoomState;
import com.tuempresa.rogue.reward.RewardSystem;
import com.tuempresa.rogue.util.RogueLogger;
import com.tuempresa.rogue.util.TP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class DungeonRun {
    private final UUID id = UUID.randomUUID();
    private final DungeonDef def;
    private final List<RoomState> rooms = new ArrayList<>();
    private final Set<UUID> party = new LinkedHashSet<>();
    private int currentRoomIndex;
    private int ticksInRoom;
    private boolean finished;
    private boolean victory;

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

    public void addMember(ServerPlayer player) {
        party.add(player.getUUID());
    }

    public void tick(MinecraftServer server) {
        if (finished) {
            return;
        }
        ticksInRoom++;
        if (ticksInRoom >= RogueConfig.roomTimeLimitTicks()) {
            finishSuccess(server);
        }
    }

    public void spawnWave() {
        RoomState room = getCurrentRoom();
        if (room != null) {
            room.setAlive(room.getAlive() + 1);
        }
    }

    public int countAlive() {
        RoomState room = getCurrentRoom();
        return room != null ? room.getAlive() : 0;
    }

    public void advanceRoom() {
        ticksInRoom = 0;
        if (currentRoomIndex + 1 < rooms.size()) {
            currentRoomIndex++;
        }
    }

    public void finishSuccess(MinecraftServer server) {
        if (finished) {
            return;
        }
        finished = true;
        victory = true;
        RewardSystem.grantRewards(server, this);
        openExitPortal();
        returnAllToCity(server);
    }

    public void finishFail(MinecraftServer server) {
        if (finished) {
            return;
        }
        finished = true;
        victory = false;
        returnAllToCity(server);
    }

    public void cleanup(MinecraftServer server) {
        returnAllToCity(server);
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isVictory() {
        return victory;
    }

    public void openExitPortal() {
        RogueLogger.info("Run {} completada, generando portal de salida.", id);
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
}
