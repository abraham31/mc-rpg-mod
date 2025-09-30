package com.tuempresa.rogue.dungeon;

import com.tuempresa.rogue.RogueMod;
import com.tuempresa.rogue.economy.Economy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Optional;
import java.util.UUID;

/**
 * Sistema encargado de repartir las recompensas al completar una mazmorra.
 */
final class RewardSystem {
    private static final int GOLD_MIN = 70;
    private static final int GOLD_MAX = 100;
    private static final int FRAGMENT_MIN = 1;
    private static final int FRAGMENT_MAX = 2;
    private static final double COSMETIC_CHANCE = 0.05D;
    private static final ResourceLocation ROCK_FRAGMENT_ID = RogueMod.id("rock_fragment");
    private static final ResourceLocation COSMETIC_TOKEN_ID = RogueMod.id("cosmetic_token");

    private RewardSystem() {
    }

    static void grantVictoryRewards(MinecraftServer server, DungeonRun run) {
        RandomSource random = RandomSource.create();
        for (UUID memberId : run.instance().members()) {
            ServerPlayer player = server.getPlayerList().getPlayer(memberId);
            if (player == null) {
                continue;
            }

            int gold = random.nextIntBetweenInclusive(GOLD_MIN, GOLD_MAX);
            Economy.giveGold(player, gold);

            giveRockFragments(player, random);
            maybeGiveCosmetic(player, random);

            player.sendSystemMessage(Component.literal("Recompensas de mazmorra: +" + gold + " de oro."));
        }
    }

    private static void giveRockFragments(ServerPlayer player, RandomSource random) {
        Item fragmentItem = resolveItem(ROCK_FRAGMENT_ID).orElse(Items.AMETHYST_SHARD);
        int amount = random.nextIntBetweenInclusive(FRAGMENT_MIN, FRAGMENT_MAX);
        if (amount <= 0) {
            return;
        }

        ItemStack stack = new ItemStack(fragmentItem, amount);
        giveItem(player, stack);
        player.sendSystemMessage(Component.literal("Has recibido " + amount + " Fragmentos de Roca."));
    }

    private static void maybeGiveCosmetic(ServerPlayer player, RandomSource random) {
        if (random.nextDouble() >= COSMETIC_CHANCE) {
            return;
        }

        Item cosmeticItem = resolveItem(COSMETIC_TOKEN_ID).orElse(Items.NAME_TAG);
        ItemStack stack = new ItemStack(cosmeticItem);
        giveItem(player, stack);
        player.sendSystemMessage(Component.literal("¡Suerte! Obtienes un cosmético especial."));
    }

    private static Optional<Item> resolveItem(ResourceLocation id) {
        return BuiltInRegistries.ITEM.getOptional(id);
    }

    private static void giveItem(ServerPlayer player, ItemStack stack) {
        boolean added = player.addItem(stack);
        if (!added) {
            player.drop(stack, false);
        }
    }
}
