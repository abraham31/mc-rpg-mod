package com.tuempresa.rogue.core;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.tuempresa.rogue.core.command.RogueConfigCommand;
import com.tuempresa.rogue.core.command.RogueDataCommand;
import com.tuempresa.rogue.core.command.RogueGoldCommand;
import com.tuempresa.rogue.core.command.RogueRunsCommand;
import com.tuempresa.rogue.core.command.RogueWarpCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class RogueCommandEvents {
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(RogueConstants.MOD_ID)
            .requires(source -> source.hasPermission(2));

        RogueDataCommand.register(root);
        RogueGoldCommand.register(root);
        RogueRunsCommand.register(root);
        RogueWarpCommand.register(root);
        RogueConfigCommand.register(root);

        event.getDispatcher().register(root);
    }
}
