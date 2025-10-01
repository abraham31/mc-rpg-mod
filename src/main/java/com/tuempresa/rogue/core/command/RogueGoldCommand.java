package com.tuempresa.rogue.core.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.tuempresa.rogue.economy.Economy;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class RogueGoldCommand {
    private RogueGoldCommand() {
    }

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("gold")
            .then(Commands.literal("get")
                .then(Commands.argument("jugador", EntityArgument.player())
                    .executes(ctx -> getGold(ctx.getSource(), EntityArgument.getPlayer(ctx, "jugador")))))
            .then(Commands.literal("add")
                .then(Commands.argument("jugador", EntityArgument.player())
                    .then(Commands.argument("cantidad", IntegerArgumentType.integer(0))
                        .executes(ctx -> addGold(
                            ctx.getSource(),
                            EntityArgument.getPlayer(ctx, "jugador"),
                            IntegerArgumentType.getInteger(ctx, "cantidad")))))));
    }

    private static int getGold(CommandSourceStack source, ServerPlayer player) {
        int gold = Economy.getGold(player);
        source.sendSuccess(() -> Component.literal(player.getName().getString() + " tiene " + gold + " de oro."), false);
        return gold;
    }

    private static int addGold(CommandSourceStack source, ServerPlayer player, int amount) {
        int total = Economy.giveGold(player, amount);
        source.sendSuccess(() -> Component.literal("Se añadieron " + amount + " de oro. Total=" + total), true);
        if (source.getEntity() != player) {
            player.sendSystemMessage(Component.literal("Has recibido " + amount + " de oro."));
        }
        return total;
    }
}
