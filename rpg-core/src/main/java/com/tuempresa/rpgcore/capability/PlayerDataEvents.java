package com.tuempresa.rpgcore.capability;

import com.tuempresa.rpgcore.util.SyncUtil;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class PlayerDataEvents {
  private PlayerDataEvents() {}

  @SubscribeEvent
  public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
    if (e.getEntity() instanceof ServerPlayer sp) SyncUtil.sync(sp);
  }

  @SubscribeEvent
  public static void onClone(PlayerEvent.Clone e) {
    if (e.getEntity() instanceof ServerPlayer sp && e.getOriginal() != null) {
      SyncUtil.sync(sp);
    }
  }
}
