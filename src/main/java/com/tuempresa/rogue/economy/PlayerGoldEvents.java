package com.tuempresa.rogue.economy;

import com.tuempresa.rogue.RogueMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.AttachCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@Mod.EventBusSubscriber(modid = RogueMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlayerGoldEvents {
    private PlayerGoldEvents() {
    }

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            PlayerGoldProvider provider = new PlayerGoldProvider();
            event.addCapability(PlayerGoldProvider.ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(RogueCapabilities.PLAYER_GOLD).ifPresent(oldStore ->
            event.getEntity().getCapability(RogueCapabilities.PLAYER_GOLD).ifPresent(newStore -> newStore.copyFrom(oldStore)));
        event.getOriginal().invalidateCaps();
    }
}
