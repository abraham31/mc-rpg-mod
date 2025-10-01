package com.tuempresa.rogue.core.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.tuempresa.rogue.dungeon.DungeonManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class RogueWarpCommand {
    private RogueWarpCommand() {
    }

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("warp")
            .then(Commands.argument("roomId", StringArgumentType.string())
                .executes(ctx -> warp(ctx.getSource(), StringArgumentType.getString(ctx, "roomId")))));
    }

    private static int warp(CommandSourceStack source, String roomId) {
        try {
            boolean warped = DungeonManager.warpPlayerToRoom(source.getPlayerOrException(), roomId);
            if (!warped) {
                source.sendFailure(Component.literal("No se encontró la sala " + roomId));
                return 0;
            }
            source.sendSuccess(() -> Component.literal("Teletransportado a " + roomId), false);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error: " + e.getMessage()));
            return 0;
        }
    }
}
