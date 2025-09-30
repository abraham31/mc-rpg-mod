package com.tuempresa.rogue.economy;

import com.tuempresa.rogue.RogueMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capability;
import net.neoforged.neoforge.capabilities.CapabilityManager;
import net.neoforged.neoforge.capabilities.CapabilityToken;
import net.neoforged.neoforge.event.RegisterCapabilitiesEvent;

@Mod.EventBusSubscriber(modid = RogueMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RogueCapabilities {
    public static final Capability<PlayerGold> PLAYER_GOLD = CapabilityManager.get(new CapabilityToken<>() {
    });

    private RogueCapabilities() {
    }

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.register(PlayerGold.class);
    }
}
