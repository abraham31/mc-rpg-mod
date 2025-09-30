package com.tuempresa.rogue.economy;

import net.minecraft.world.entity.player.Player;

public final class Economy {
    private Economy() {
    }

    public static int getGold(Player player) {
        return player.getCapability(RogueCapabilities.PLAYER_GOLD)
            .map(PlayerGold::getGold)
            .orElse(0);
    }

    public static int giveGold(Player player, int amount) {
        return player.getCapability(RogueCapabilities.PLAYER_GOLD)
            .map(store -> {
                store.addGold(amount);
                return store.getGold();
            })
            .orElse(0);
    }

    public static boolean takeGold(Player player, int amount) {
        return player.getCapability(RogueCapabilities.PLAYER_GOLD)
            .map(store -> store.removeGold(amount))
            .orElse(false);
    }

    public static boolean hasGold(Player player, int amount) {
        return player.getCapability(RogueCapabilities.PLAYER_GOLD)
            .map(store -> store.hasGold(amount))
            .orElse(false);
    }
}
