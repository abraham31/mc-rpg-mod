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
  public void onLevelLoad(LevelEvent.Load e) {
    if (!(e.getLevel() instanceof ServerLevel level)) return;

    String id = level.dimension().location().toString();
    // define el diámetro por dimensión
    double diameter =
        switch (id) {
          case "rpg_content_prontera:city" -> 480.0; // ~240x240
          case "rpg_content_prontera:field1" -> 1024.0; // ~512x512
          case "rpg_content_prontera:field2" -> 1024.0; // ~512x512
          default -> -1.0;
        };

    if (diameter > 0) {
      var wb = level.getWorldBorder();
      wb.setCenter(0.0, 0.0);
      wb.setSize(diameter); // diámetro, no radio
      wb.setDamageAmount(0.2); // opcional
      wb.setWarningBlocks(6); // opcional
    }
  }
}
