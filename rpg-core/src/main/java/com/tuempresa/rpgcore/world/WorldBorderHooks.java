package com.tuempresa.rpgcore.world;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.minecraft.server.level.ServerLevel;

public final class WorldBorderHooks {
  public static void register() {
    NeoForge.EVENT_BUS.register(new WorldBorderHooks());
  }

  @SubscribeEvent
  public void onLevelLoad(LevelEvent.Load event) {
    if (event.getLevel() instanceof ServerLevel level) {
      WorldBorderConfig.apply(level);
    }
  }
}
