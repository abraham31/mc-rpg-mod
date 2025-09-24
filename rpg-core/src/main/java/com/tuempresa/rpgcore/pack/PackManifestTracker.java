package com.tuempresa.rpgcore.pack;

import com.tuempresa.rpgcore.ModRpgCore;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * Gestiona la actualización del {@link PackManifestData} cada vez que arranca
 * un servidor dedicado o mundo local.
 */
public final class PackManifestTracker {
  private PackManifestTracker() {}

  public static void register() {
    NeoForge.EVENT_BUS.register(new PackManifestTracker());
  }

  @SubscribeEvent
  public void onServerStarted(ServerStartedEvent event) {
    MinecraftServer server = event.getServer();
    ServerLevel overworld = server.getLevel(Level.OVERWORLD);
    if (overworld == null) {
      return;
    }

    PackManifestData data = PackManifestData.get(overworld);
    data.updateFromModList();
    ModRpgCore.LOG.info("Packs activos detectados: {}", data.getPacks());
  }
}
