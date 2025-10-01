package com.tuempresa.rogue.reward;

import com.tuempresa.rogue.data.model.RewardsDef;
import com.tuempresa.rogue.dungeon.instance.DungeonRun;
import com.tuempresa.rogue.economy.Economy;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class RewardSystem {
    private RewardSystem() {
    }

    public static void grantRewards(MinecraftServer server, DungeonRun run) {
        RewardsDef rewards = run.getDef().rewards();
        RandomSource random = RandomSource.create();
        run.getParty().forEach(uuid -> {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                giveGold(player, random, rewards);
                giveMaterial(player, random, rewards);
                rollCosmetic(player, random, rewards);
            }
        });
    }

    private static void giveGold(ServerPlayer player, RandomSource random, RewardsDef rewards) {
        int amount = random.nextIntBetweenInclusive(rewards.goldMin(), rewards.goldMax());
        Economy.giveGold(player, amount);
    }

    private static void giveMaterial(ServerPlayer player, RandomSource random, RewardsDef rewards) {
        Item item = BuiltInRegistries.ITEM.getOptional(rewards.materialId()).orElse(Items.AMETHYST_SHARD);
        int amount = random.nextIntBetweenInclusive(rewards.materialMin(), rewards.materialMax());
        if (amount <= 0) {
            return;
        }
        ItemStack stack = new ItemStack(item, amount);
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    private static void rollCosmetic(ServerPlayer player, RandomSource random, RewardsDef rewards) {
        if (random.nextDouble() <= rewards.cosmeticChance()) {
            ItemStack cosmetic = new ItemStack(Items.NAME_TAG);
            if (!player.addItem(cosmetic)) {
                player.drop(cosmetic, false);
            }
        }
    }
}
