package com.tuempresa.rogue.portal;

import com.tuempresa.rogue.core.RogueMod;
import com.tuempresa.rogue.data.model.DungeonDef;
import com.tuempresa.rogue.dungeon.DungeonManager;
import com.tuempresa.rogue.dungeon.instance.DungeonRun;
import com.tuempresa.rogue.economy.Economy;
import com.tuempresa.rogue.util.Chat;
import com.tuempresa.rogue.util.TP;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;

public final class PortalSystem {
    private static final PortalSystem INSTANCE = new PortalSystem();

    private PortalSystem() {
    }

    public static PortalSystem get() {
        return INSTANCE;
    }

    public InteractionResult tryEnter(ServerPlayer player, String dungeonId) {
        DungeonDef def = RogueMod.DUNGEON_DATA.getDungeon(dungeonId).orElse(null);
        if (def == null) {
            Chat.error(player, "Mazmorra desconocida: " + dungeonId);
            return InteractionResult.FAIL;
        }
        if (!meetsLevel(player, def.levelMin())) {
            Chat.error(player, "Nivel requerido: " + def.levelMin());
            return InteractionResult.FAIL;
        }
        if (!chargeEntry(player, def.entryCost())) {
            return InteractionResult.FAIL;
        }
        DungeonRun run = DungeonManager.createOrJoin(def, player);
        ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, def.world());
        TP.toSpawn(player, levelKey);
        run.spawnWave();
        Chat.success(player, "Entrando a " + def.id());
        return InteractionResult.SUCCESS;
    }

    public boolean meetsLevel(ServerPlayer player, int required) {
        return player.experienceLevel >= required;
    }

    public boolean chargeEntry(ServerPlayer player, int cost) {
        if (cost <= 0) {
            return true;
        }
        if (!Economy.hasGold(player, cost)) {
            Chat.error(player, "Necesitas " + cost + " de oro.");
            return false;
        }
        return Economy.takeGold(player, cost);
    }

    public DungeonRun createOrJoinRun(DungeonDef def, ServerPlayer player) {
        return DungeonManager.createOrJoin(def, player);
    }
}
