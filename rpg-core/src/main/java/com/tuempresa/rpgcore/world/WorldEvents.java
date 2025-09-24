package com.tuempresa.rpgcore.world;

import com.tuempresa.rpgcore.ModRpgCore;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

public final class WorldEvents {
  private WorldEvents() {}

  public static void register() {
    NeoForge.EVENT_BUS.register(new WorldEvents());
  }

  @SubscribeEvent
  public void onLevelLoad(LevelEvent.Load event) {
    if (!(event.getLevel() instanceof ServerLevel level)) {
      return;
    }

    String key = level.dimension().location().toString();
    int radius = switch (key) {
      case "rpg_content_base:city" -> 120;
      case "rpg_content_base:field1", "rpg_content_base:field2" -> 256;
      default -> -1;
    };

    if (radius > 0) {
      var border = level.getWorldBorder();
      border.setCenter(0, 0);
      border.setSize(radius * 2L);
      ModRpgCore.LOG.debug("Aplicando world border de {} bloques al mundo {}", radius, key);
    }
  }

  @SubscribeEvent
  public void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
    if (!(event.getLevel() instanceof ServerLevel level)) {
      return;
    }

    String dimensionId = level.dimension().location().toString();
    if ("rpg_content_base:field1".equals(dimensionId)) {
      var type = event.getEntity().getType();
      boolean allow = type == EntityType.SLIME || type == EntityType.RABBIT;
      if (!allow) {
        event.setCanceled(true);
      }
      return;
    }

    if ("rpg_content_base:field2".equals(dimensionId)) {
      var type = event.getEntity().getType();
      boolean allow = type == EntityType.WOLF || type == EntityType.SPIDER;
      if (!allow) {
        event.setCanceled(true);
      }
    }
  }
}
