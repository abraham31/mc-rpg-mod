package com.tuempresa.rpgcore.net;

import java.util.Objects;

import com.tuempresa.rpgcore.ModRpgCore;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record S2CPlayerData(String classId, int level, long xp, long currency) implements CustomPacketPayload {
    public S2CPlayerData {
        classId = Objects.requireNonNull(classId, "classId");
    }
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ModRpgCore.MOD_ID, "player_data");
    public static final CustomPacketPayload.Type<S2CPlayerData> TYPE = new CustomPacketPayload.Type<>(ID);

    public static S2CPlayerData decode(FriendlyByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");
        String classId = buffer.readUtf(FriendlyByteBuf.MAX_STRING_LENGTH);
        int level = buffer.readVarInt();
        long xp = buffer.readVarLong();
        long currency = buffer.readVarLong();
        return new S2CPlayerData(classId, level, xp, currency);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        Objects.requireNonNull(buffer, "buffer");
        buffer.writeUtf(classId, FriendlyByteBuf.MAX_STRING_LENGTH);
        buffer.writeVarInt(level);
        buffer.writeVarLong(xp);
        buffer.writeVarLong(currency);
    }

    public static final class ClientCache {
        private static volatile S2CPlayerData lastSnapshot = new S2CPlayerData("", 1, 0L, 0L);

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
