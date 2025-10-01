package com.tuempresa.rogue.core.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.tuempresa.rogue.util.RogueLogger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class RogueConfigCommand {
    private RogueConfigCommand() {
    }

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("config")
            .then(Commands.literal("reload").executes(ctx -> reload(ctx.getSource()))));
    }

    private static int reload(CommandSourceStack source) {
        RogueLogger.info("Recarga manual solicitada de rogue-common.toml");
        source.sendSuccess(() -> Component.literal("La configuración se volverá a aplicar al reiniciar."), false);
        return 1;
    }
}
