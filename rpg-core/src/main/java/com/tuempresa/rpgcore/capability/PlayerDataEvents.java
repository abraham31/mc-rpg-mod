package com.tuempresa.rpgcore.capability;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import com.tuempresa.rpgcore.ModRpgCore;
import com.tuempresa.rpgcore.net.Net;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.EntityEvent.Attachments.Register;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class PlayerDataEvents {
    private static final Marker MARKER = MarkerManager.getMarker("PlayerDataEvents");

    private PlayerDataEvents() {
    }

    public static void registerPlayerData(Register event) {
        event.register(EntityType.PLAYER, PlayerDataAttachment.TYPE);
        ModRpgCore.LOG.debug(MARKER, "Registered PlayerData attachment for players");
    }

    public static void syncOnLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer) {
            Net.sync(serverPlayer);
        }
    }

    public static void handlePlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player clone = event.getEntity();

        PlayerData originalData = PlayerData.get(original);
        PlayerData cloneData = PlayerData.get(clone);
        cloneData.copyFrom(originalData);
        ModRpgCore.LOG.debug(MARKER, "Copied PlayerData from {} to {}", original.getGameProfile().getName(), clone.getGameProfile().getName());
        if (event.isWasDeath() && clone instanceof ServerPlayer serverPlayer) {
            Net.sync(serverPlayer);
        }
    }
}
