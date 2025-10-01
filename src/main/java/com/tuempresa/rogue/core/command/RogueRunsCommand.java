package com.tuempresa.rogue.core.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.tuempresa.rogue.dungeon.DungeonManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class RogueRunsCommand {
    private RogueRunsCommand() {
    }

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("runs")
            .then(Commands.literal("list").executes(ctx -> listRuns(ctx.getSource())))
            .then(Commands.literal("cleanup").executes(ctx -> cleanup(ctx.getSource()))));
    }

    private static int listRuns(CommandSourceStack source) {
        var runs = DungeonManager.listRuns();
        if (runs.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No hay runs activos."), false);
            return 0;
        }
        runs.forEach(info -> source.sendSuccess(() -> Component.literal(
            String.format("- %s miembros=%d estado=%s", info.dungeonId(), info.partySize(), info.victory() ? "victoria" : "en curso")), false));
        return runs.size();
    }

    private static int cleanup(CommandSourceStack source) {
        int removed = DungeonManager.cleanupFinishedRuns();
        source.sendSuccess(() -> Component.literal("Se limpiaron " + removed + " runs."), true);
        return removed;
    }
}
