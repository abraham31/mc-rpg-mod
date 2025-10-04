package com.tuempresa.rogue.portal;

import com.tuempresa.rogue.RogueMod;
import com.tuempresa.rogue.data.model.DungeonDef;
import com.tuempresa.rogue.dungeon.DungeonManager;
import com.tuempresa.rogue.dungeon.instance.DungeonRun;
import com.tuempresa.rogue.economy.Economy;
import com.tuempresa.rogue.util.Chat;
import com.tuempresa.rogue.util.RogueLogger;
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
            Chat.error(player, "Necesitas nivel " + def.levelMin() + " para usar este portal (actual: "
                + player.experienceLevel + ").");
            return InteractionResult.FAIL;
        }
        if (!chargeEntry(player, def.entryCost())) {
            return InteractionResult.FAIL;
        }
        DungeonRun run = DungeonManager.createOrJoin(def, player);
        ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, def.world());
        if (player.server == null) {
            Chat.error(player, "El portal no está disponible en este momento. Inténtalo más tarde.");
            return InteractionResult.FAIL;
        }
        if (player.server.getLevel(levelKey) == null) {
            RogueLogger.error("No se encontró el nivel {} para el portal {}", def.world(), def.id());
            Chat.error(player, "El destino del portal aún no está cargado. Reintenta en unos segundos.");
            return InteractionResult.FAIL;
        }
        TP.toSpawn(player, levelKey);
        try {
            run.spawnWave(player.server);
        } catch (Exception exception) {
            RogueLogger.error("Error al iniciar la mazmorra {}", def.id(), exception);
            Chat.error(player, "El portal falló al estabilizarse. Consulta con un administrador.");
            return InteractionResult.FAIL;
        }
        Chat.success(player, "¡Portal conectado! Destino: " + def.id());
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
            Chat.error(player, "Necesitas " + cost + " de oro para activar el portal (tienes "
                + Economy.getGold(player) + ").");
            return false;
        }
        if (!Economy.takeGold(player, cost)) {
            Chat.error(player, "No se pudo cobrar la entrada. Intenta de nuevo.");
            return false;
        }
        Chat.tip(player, "Has pagado " + cost + " de oro para estabilizar el portal.");
        return true;
    }

    public DungeonRun createOrJoinRun(DungeonDef def, ServerPlayer player) {
        return DungeonManager.createOrJoin(def, player);
    }
}
