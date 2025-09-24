package com.tuempresa.rpgcore.world;

import com.tuempresa.rpgcore.ModRpgCore;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
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
      case "rpg_content_prontera:city" -> 120;
      case "rpg_content_prontera:field1", "rpg_content_prontera:field2" -> 256;
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
  public void onEntityJoin(EntityJoinLevelEvent event) {
    if (!(event.getLevel() instanceof ServerLevel level)) {
      return;
    }

    if (!(event.getEntity() instanceof Mob mob)) {
      return;
    }

    String dimensionId = level.dimension().location().toString();
    if ("rpg_content_prontera:field1".equals(dimensionId)) {
      var type = mob.getType();
      boolean allow = type == EntityType.SLIME || type == EntityType.RABBIT;
      if (!allow) {
        event.setCanceled(true);
      }
      return;
    }

    if ("rpg_content_prontera:field2".equals(dimensionId)) {
      var type = mob.getType();
      boolean allow = type == EntityType.WOLF || type == EntityType.SPIDER;
      if (!allow) {
        event.setCanceled(true);
      }
      return;
    }

    if ("rpg_content_prontera:city".equals(dimensionId)) {
      event.setCanceled(true);
    }
  }
}
