package com.tuempresa.rpgcore.net;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import com.tuempresa.rpgcore.ModRpgCore;
import com.tuempresa.rpgcore.capability.PlayerData;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.NetworkDirection;
import net.neoforged.neoforge.network.NetworkRegistry;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.simple.SimpleChannel;

public final class Net {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath(ModRpgCore.MOD_ID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

    private Net() {
    }

    public static void init() {
        CHANNEL.messageBuilder(S2CPlayerData.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
            .encoder(S2CPlayerData::encode)
            .decoder(S2CPlayerData::decode)
            .consumerMainThread(S2CPlayerData::handle)
            .add();
    }

    private static int nextId() {
        return ID_GENERATOR.getAndIncrement();
    }

    public static void sync(Player player) {
        Objects.requireNonNull(player, "Player cannot be null");
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        PlayerData data = PlayerData.get(serverPlayer);
        S2CPlayerData payload = new S2CPlayerData(data.getClassId(), data.getLevel(), data.getXp(), data.getCurrency());
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), payload);
    }

    static SimpleChannel getChannel() {
        return CHANNEL;
    }
}
