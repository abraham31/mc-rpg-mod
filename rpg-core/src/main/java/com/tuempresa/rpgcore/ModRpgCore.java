package com.tuempresa.rpgcore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.tuempresa.rpgcore.capability.PlayerDataEvents;
import com.tuempresa.rpgcore.command.RpgCommands;
import com.tuempresa.rpgcore.net.Net;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AttachCapabilitiesEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@Mod(ModRpgCore.MOD_ID)
public final class ModRpgCore {
    public static final String MOD_ID = "rpg_core";
    public static final Logger LOG = LogManager.getLogger("RPG-Core");

    // ✅ En 1.21 el bus del MOD llega por el constructor
    public ModRpgCore(IEventBus modEventBus) {
        LOG.info("[RPG-Core] Cargando mod…");

        // Si quieres escuchar eventos del bus del MOD (registries, setup, etc.)
        modEventBus.addListener(this::commonSetup);

        // Y para eventos del JUEGO (como RegisterCommandsEvent), te registras en NeoForge.EVENT_BUS
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOG.info("[RPG-Core] commonSetup OK");
        event.enqueueWork(Net::init);
    }

    // Evento del GAME bus (no del mod bus)
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent e) {
        RpgCommands.register(e);
    }

    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Player> event) {
        PlayerDataEvents.attachPlayerData(event);
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        PlayerDataEvents.handlePlayerClone(event);
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        PlayerDataEvents.syncOnLogin(event);
    }
}
