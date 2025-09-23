package com.tuempresa.rpgcore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

@Mod(ModRpgCore.MOD_ID)
public final class ModRpgCore {
    public static final String MOD_ID = "rpg_core";
    public static final Logger LOG = LogManager.getLogger("RPG-Core");

    // ✅ En 1.21 el bus del MOD llega por el constructor
    public ModRpgCore(IEventBus modEventBus) {
        LOG.info("[RPG-Core] Cargando mod…");

        // Si quieres escuchar eventos del bus del MOD (registries, setup, etc.)
        modEventBus.addListener(this::commonSetup);

        // Y para eventos del JUEGO (como RegisterCommandsEvent), te registras en NeoForge.EVENT_BUS
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOG.info("[RPG-Core] commonSetup OK");
    }

    // Evento del GAME bus (no del mod bus)
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent e) {
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
