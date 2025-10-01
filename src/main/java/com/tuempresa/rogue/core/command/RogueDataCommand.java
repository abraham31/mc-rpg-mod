package com.tuempresa.rogue.core.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.tuempresa.rogue.core.RogueMod;
import com.tuempresa.rogue.data.DungeonDataException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class RogueDataCommand {
    private RogueDataCommand() {
    }

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("reload").executes(context -> reload(context.getSource())));
        root.then(Commands.literal("dungeons").then(Commands.literal("list").executes(context -> list(context.getSource()))));
    }

    private static int reload(CommandSourceStack source) {
        try {
            RogueMod.DUNGEON_DATA.reloadNow(source.getServer());
            int size = RogueMod.DUNGEON_DATA.dungeons().size();
            source.sendSuccess(() -> Component.literal("Se recargaron " + size + " dungeons."), true);
            return size;
        } catch (DungeonDataException e) {
            source.sendFailure(Component.literal("Error al recargar: " + e.getMessage()));
            return 0;
        }
    }

    private static int list(CommandSourceStack source) {
        if (RogueMod.DUNGEON_DATA.dungeons().isEmpty()) {
            source.sendSuccess(() -> Component.literal("No hay dungeons cargadas."), false);
            return 0;
        }
        RogueMod.DUNGEON_DATA.dungeons().values().forEach(def ->
            source.sendSuccess(() -> Component.literal("- " + def.id()), false));
        return RogueMod.DUNGEON_DATA.dungeons().size();
    }
}
