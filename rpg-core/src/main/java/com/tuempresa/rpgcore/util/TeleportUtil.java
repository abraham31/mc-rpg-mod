package com.tuempresa.rpgcore.util;

import com.tuempresa.rpgcore.world.WorldBorderConfig;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class TeleportUtil {
  private TeleportUtil() {}

  public static int tpNamed(ServerPlayer player, String dimensionKey) {
    var server = player.server;
    ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimensionKey));
    ServerLevel level = server.getLevel(key);
    if (level == null) {
      return 0;
    }

    WorldBorderConfig.apply(level);

    var spawn = level.getSharedSpawnPos();
    player.teleportTo(level, spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, player.getYRot(), player.getXRot());
    return 1;
  }
}
