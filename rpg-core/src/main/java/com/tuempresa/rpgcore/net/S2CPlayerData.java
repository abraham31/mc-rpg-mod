package com.tuempresa.rpgcore.net;

import java.util.Objects;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.NetworkDirection;
import net.neoforged.neoforge.network.NetworkEvent;

public record S2CPlayerData(int classId, int level, long xp, long currency) {
    public static void encode(S2CPlayerData message, FriendlyByteBuf buffer) {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(buffer, "buffer");
        buffer.writeVarInt(message.classId());
        buffer.writeVarInt(message.level());
        buffer.writeVarLong(message.xp());
        buffer.writeVarLong(message.currency());
    }

    public static S2CPlayerData decode(FriendlyByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");
        int classId = buffer.readVarInt();
        int level = buffer.readVarInt();
        long xp = buffer.readVarLong();
        long currency = buffer.readVarLong();
        return new S2CPlayerData(classId, level, xp, currency);
    }

    public static void handle(S2CPlayerData message, Supplier<NetworkEvent.Context> contextSupplier) {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(contextSupplier, "contextSupplier");
        NetworkEvent.Context context = contextSupplier.get();
        Objects.requireNonNull(context, "context");
        context.enqueueWork(() -> {
            NetworkDirection direction = context.getDirection();
            if (direction != null && direction.getReceptionSide().isClient()) {
                ClientCache.update(message);
            }
        });
        context.setPacketHandled(true);
    }

    public static final class ClientCache {
        private static volatile S2CPlayerData lastSnapshot = new S2CPlayerData(0, 1, 0L, 0L);

        private ClientCache() {
        }

        static void update(S2CPlayerData snapshot) {
            lastSnapshot = Objects.requireNonNull(snapshot, "snapshot");
        }

        public static S2CPlayerData getLastSnapshot() {
            return lastSnapshot;
        }
    }
}
