package com.tuempresa.rpgcore.net;

import java.util.Objects;

import com.tuempresa.rpgcore.ModRpgCore;
import com.tuempresa.rpgcore.capability.PlayerData;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.RegisterPayloadHandlersEvent;

public final class Net {
    private Net() {
    }

    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        Objects.requireNonNull(event, "event");
        event.registrar(ModRpgCore.MOD_ID)
            .playToClient(S2CPlayerData.TYPE, S2CPlayerData::decode, (payload, context) ->
                context.enqueueWork(() -> S2CPlayerData.ClientCache.update(payload)));
    }

    public static void sync(Player player) {
        Objects.requireNonNull(player, "Player cannot be null");
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        PlayerData data = PlayerData.get(serverPlayer);
        S2CPlayerData payload = new S2CPlayerData(data.getClassId(), data.getLevel(), data.getXp(), data.getCurrency());
        PacketDistributor.send(serverPlayer, payload);
    }
}
