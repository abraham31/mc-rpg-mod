package com.tuempresa.rogue.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Funciones de conveniencia para teletransportar jugadores entre dimensiones.
 */
public final class TP {
    private TP() {
    }

    public static Optional<ServerLevel> level(MinecraftServer server, ResourceKey<Level> levelKey) {
        return Optional.ofNullable(server.getLevel(levelKey));
    }

    public static void to(ServerPlayer player, ResourceKey<Level> levelKey, BlockPos blockPos) {
        to(player, levelKey, Vec3.atCenterOf(blockPos));
    }

    public static void to(ServerPlayer player, ResourceKey<Level> levelKey, Vec3 position) {
        MinecraftServer server = player.server;
        if (server == null) {
            throw new IllegalStateException("No se puede teletransportar sin referencia al servidor");
        }

        ServerLevel level = server.getLevel(levelKey);
        if (level == null) {
            throw new IllegalArgumentException("No existe la dimensión " + levelKey.location());
        }

        if (player.level().dimension() == levelKey) {
            player.connection.teleport(position.x(), position.y(), position.z(), player.getYRot(), player.getXRot());
        } else {
            RogueLogger.info("Teletransportando a {} -> {}", player.getName().getString(), levelKey.location());
            player.teleportTo(level, position.x(), position.y(), position.z(), player.getYRot(), player.getXRot());
        }
    }

    public static void toSpawn(ServerPlayer player, ResourceKey<Level> levelKey) {
        MinecraftServer server = player.server;
        if (server == null) {
            throw new IllegalStateException("Servidor no disponible");
        }

        ServerLevel level = server.getLevel(levelKey);
        if (level == null) {
            throw new IllegalArgumentException("No existe la dimensión " + levelKey.location());
        }

        BlockPos pos = level.getSharedSpawnPos();
        to(player, levelKey, Vec3.atCenterOf(pos));
    }
}
