package com.tuempresa.rpgcore.net;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.PayloadRegistrar;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;

public final class Net {

  @SubscribeEvent // suscribe ESTA clase en el MOD BUS o registra una instancia
  public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
    PayloadRegistrar reg = event.registrar("1"); // versión de protocolo que tú definas

    reg.playToClient(
        S2CPlayerData.TYPE,
        S2CPlayerData.STREAM_CODEC,
        Net::handleS2CPlayerData
    );
  }

  private static void handleS2CPlayerData(S2CPlayerData payload, IPayloadContext ctx) {
    // Cliente: guarda en algún “cache” futuro (HUD). Por ahora, nada o log.
    ctx.enqueueWork(() -> {
      // TODO: ClientCache.set(payload)
    });
  }

  public static void sendTo(ServerPlayer player, S2CPlayerData payload) {
    PacketDistributor.sendToPlayer(player, payload);
  }
}
