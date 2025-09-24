package com.tuempresa.rpgcore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.tuempresa.rpgcore.capability.PlayerDataAttachment;
import com.tuempresa.rpgcore.capability.PlayerDataEvents;
import com.tuempresa.rpgcore.command.RpgCommands;
import com.tuempresa.rpgcore.net.Net;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(ModRpgCore.MOD_ID)
public final class ModRpgCore {
  public static final String MOD_ID = "rpg_core";
  public static final Logger LOG = LogManager.getLogger("RPG-Core");

  // ✅ En 1.21 el bus del MOD llega por el constructor
  public ModRpgCore(IEventBus modEventBus) {
    LOG.info("[RPG-Core] Cargando mod…");

    PlayerDataAttachment.register(modEventBus);  // ✅
    modEventBus.register(Net.class); // ✅ para que reciba RegisterPayloadHandlersEvent

    // Y para eventos del JUEGO (como RegisterCommandsEvent), te registras en NeoForge.EVENT_BUS
    NeoForge.EVENT_BUS.register(this);
    NeoForge.EVENT_BUS.register(PlayerDataEvents.class);
  }

  // Evento del GAME bus (no del mod bus)
  @SubscribeEvent
  public void onRegisterCommands(RegisterCommandsEvent e) {
    RpgCommands.register(e);
  }
}
