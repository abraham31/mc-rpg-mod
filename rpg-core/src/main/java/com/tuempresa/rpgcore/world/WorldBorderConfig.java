package com.tuempresa.rpgcore.world;

import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.border.WorldBorder;

public final class WorldBorderConfig {
  private static final Map<ResourceLocation, Integer> BORDER_SIZES =
      Map.ofEntries(
          Map.entry(ResourceLocation.fromNamespaceAndPath("rpg_content_prontera", "city"), 128),
          Map.entry(ResourceLocation.fromNamespaceAndPath("rpg_content_prontera", "field1"), 512),
          Map.entry(ResourceLocation.fromNamespaceAndPath("rpg_content_prontera", "field2"), 512),
          Map.entry(ResourceLocation.fromNamespaceAndPath("rpg_content_prontera", "dungeon1"), 192));

  private WorldBorderConfig() {}

  public static void apply(ServerLevel level) {
    if (level == null) {
      return;
    }

    WorldBorder border = level.getWorldBorder();
    ResourceLocation dimension = level.dimension().location();
    Integer size = BORDER_SIZES.get(dimension);

    if (size == null) {
      return;
    }

    border.setCenter(0.0D, 0.0D);
    border.setSize(size.doubleValue());
    border.setWarningBlocks(3);
  }
}
