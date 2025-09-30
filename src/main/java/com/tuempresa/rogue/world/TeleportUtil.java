package com.tuempresa.rogue.world;

import com.tuempresa.rogue.RogueMod;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Helper methods that simplify teleporting players between the custom
 * dimensions exposed by the mod. They perform the common lookup logic and
 * handle intra-dimensional teleportation with the correct networking calls.
 */
public final class TeleportUtil {
    private TeleportUtil() {
    }

    /**
     * Retrieves the server level associated with the supplied key.
     *
     * @param server the active Minecraft server
     * @param levelKey the level key to look up
     * @return an optional containing the server level if it is loaded
     */
    public static Optional<ServerLevel> level(MinecraftServer server, ResourceKey<Level> levelKey) {
        return Optional.ofNullable(server.getLevel(levelKey));
    }

    /**
     * Teleports the player to the requested block position inside the given
     * level while preserving their current rotation.
     */
    public static void teleport(ServerPlayer player, ResourceKey<Level> levelKey, BlockPos position) {
        teleport(player, levelKey, Vec3.atCenterOf(position), player.getYRot(), player.getXRot());
    }

    /**
     * Teleports the player to the requested coordinates, changing dimensions
     * if necessary.
     *
     * @param player the player that should be moved
     * @param levelKey the destination level key
     * @param targetPosition the exact destination coordinates
     * @param yaw the horizontal rotation that should be applied on arrival
     * @param pitch the vertical rotation that should be applied on arrival
     */
    public static void teleport(ServerPlayer player, ResourceKey<Level> levelKey, Vec3 targetPosition, float yaw, float pitch) {
        MinecraftServer server = player.server;
        if (server == null) {
            throw new IllegalStateException("No se pudo acceder al servidor para teletransportar al jugador");
        }

        ServerLevel targetLevel = server.getLevel(levelKey);
        if (targetLevel == null) {
            throw new IllegalArgumentException("Nivel no encontrado para la dimensión " + levelKey.location());
        }

        if (player.level().dimension() == levelKey) {
            player.connection.teleport(targetPosition.x(), targetPosition.y(), targetPosition.z(), yaw, pitch);
        } else {
            RogueMod.LOGGER.debug("Teletransportando a {} hacia {} en {}", player.getScoreboardName(), targetPosition, levelKey.location());
            player.teleportTo(targetLevel, targetPosition.x(), targetPosition.y(), targetPosition.z(), yaw, pitch);
        }
    }
}
