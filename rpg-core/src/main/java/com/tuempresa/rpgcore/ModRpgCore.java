package com.tuempresa.rpgcore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.tuempresa.rpgcore.api.QuestService;
import com.tuempresa.rpgcore.api.WarpService;
import com.tuempresa.rpgcore.capability.PlayerDataAttachment;
import com.tuempresa.rpgcore.capability.PlayerDataEvents;
import com.tuempresa.rpgcore.command.RpgCommands;
import com.tuempresa.rpgcore.net.Net;
import com.tuempresa.rpgcore.pack.PackManifestTracker;
import com.tuempresa.rpgcore.world.WorldBorderHooks;
import com.tuempresa.rpgcore.world.WorldEvents;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.fml.ModList;

@Mod(ModRpgCore.MOD_ID)
public final class ModRpgCore {
  public static final String MOD_ID = "rpg_core";
  public static final Logger LOG = LogManager.getLogger("RPG-Core");

  // ✅ En 1.21 el bus del MOD llega por el constructor
  public ModRpgCore(IEventBus modEventBus) {
    LOG.info("[RPG-Core] Cargando mod…");

    PlayerDataAttachment.register(modEventBus);  // ✅
    modEventBus.register(Net.class); // ✅ para que reciba RegisterPayloadHandlersEvent

    QuestService.register();
    PackManifestTracker.register();

    registerDefaultWarps();

    // Y para eventos del JUEGO (como RegisterCommandsEvent), te registras en NeoForge.EVENT_BUS
    NeoForge.EVENT_BUS.register(this);
    PlayerDataEvents.registerOnGameBus();
    WorldEvents.register();
    WorldBorderHooks.register();
  }

  private static void registerDefaultWarps() {
    if (!ModList.get().isLoaded("rpg_content_prontera")) {
      LOG.warn(
          "El pack rpg-content-prontera no está presente; se omite el registro de warps por defecto.");
      return;
    }

    registerWarp("prontera/city");
    registerWarp("prontera/field1");
    registerWarp("prontera/field2");
  }

  private static void registerWarp(String path) {
    int slash = path.indexOf('/');
    if (slash < 0 || slash == path.length() - 1) {
      return;
    }
    String destination = path.substring(slash + 1);
    ResourceLocation location = ResourceLocation.fromNamespaceAndPath("rpg_content_prontera", destination);
    ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, location);
    WarpService.registerWarp(path, key);
  }

  // Evento del GAME bus (no del mod bus)
  @SubscribeEvent
  public void onRegisterCommands(RegisterCommandsEvent e) {
    RpgCommands.register(e);
  }
}
