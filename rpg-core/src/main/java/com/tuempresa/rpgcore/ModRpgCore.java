package com.tuempresa.rpgcore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.bus.api.IEventBus;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

@Mod("rpg_core")
public final class ModRpgCore {
    public static final Logger LOG = LogManager.getLogger("RPG-Core");

    public ModRpgCore() {
        LOG.info("[RPG-Core] Cargando mod…");
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::commonSetup);

        // Suscribe esta instancia al bus del juego (eventos como RegisterCommandsEvent)
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOG.info("[RPG-Core] commonSetup OK");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent e) {
        // /rpg ping  -> responde "pong"
        LiteralArgumentBuilder<CommandSourceStack> rpg = Commands.literal("rpg")
            .requires(src -> src.hasPermission(0))
            .then(Commands.literal("ping").executes(ctx -> {
                ctx.getSource().sendSuccess(() -> Component.literal("[RPG] pong 🏹"), false);
                return 1;
            }))
            .then(Commands.literal("debug").executes(ctx -> {
                int online = ctx.getSource().getServer().getPlayerList().getPlayerCount();
                ctx.getSource().sendSuccess(() -> Component.literal("[RPG] Debug OK. Online: " + online), false);
                return 1;
            }));

        e.getDispatcher().register(rpg);
    }
}
