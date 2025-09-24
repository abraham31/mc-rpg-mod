package com.tuempresa.rpgcore.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record S2CPlayerData(String clazz, int level, long xp, long currency) implements CustomPacketPayload {
  public static final Type<S2CPlayerData> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath("rpg_core", "s2c_player_data"));

  public static final StreamCodec<ByteBuf, S2CPlayerData> STREAM_CODEC =
      StreamCodec.composite(
          ByteBufCodecs.STRING_UTF8, S2CPlayerData::clazz,
          ByteBufCodecs.VAR_INT,     S2CPlayerData::level,
          ByteBufCodecs.VAR_LONG,    S2CPlayerData::xp,
          ByteBufCodecs.VAR_LONG,    S2CPlayerData::currency,
          S2CPlayerData::new
      );

  @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
