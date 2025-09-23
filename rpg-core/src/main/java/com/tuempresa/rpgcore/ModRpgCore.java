package com.tuempresa.rpgcore;

import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.tuempresa.rpgcore.capability.PlayerData;
import com.tuempresa.rpgcore.net.Net;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

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
        event.enqueueWork(Net::init);
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
            }))
            .then(buildPlayerDataCommands());

        e.getDispatcher().register(rpg);
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildPlayerDataCommands() {
        return Commands.literal("data")
            .requires(src -> src.hasPermission(2))
            .then(Commands.literal("class")
                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                    .executes(ctx -> mutatePlayerData(ctx,
                        data -> data.setClassId(IntegerArgumentType.getInteger(ctx, "value")),
                        data -> Component.literal("[RPG] Clase asignada: " + data.getClassId())))))
            .then(Commands.literal("level")
                .then(Commands.argument("value", IntegerArgumentType.integer(1))
                    .executes(ctx -> mutatePlayerData(ctx,
                        data -> data.setLevel(IntegerArgumentType.getInteger(ctx, "value")),
                        data -> Component.literal("[RPG] Nivel establecido: " + data.getLevel())))))
            .then(Commands.literal("xp")
                .then(Commands.argument("value", LongArgumentType.longArg(0))
                    .executes(ctx -> mutatePlayerData(ctx,
                        data -> data.setXp(LongArgumentType.getLong(ctx, "value")),
                        data -> Component.literal("[RPG] XP sincronizada: " + data.getXp())))))
            .then(Commands.literal("currency")
                .then(Commands.argument("value", LongArgumentType.longArg(0))
                    .executes(ctx -> mutatePlayerData(ctx,
                        data -> data.setCurrency(LongArgumentType.getLong(ctx, "value")),
                        data -> Component.literal("[RPG] Monedas sincronizadas: " + data.getCurrency())))));
    }

    private int mutatePlayerData(CommandContext<CommandSourceStack> ctx, Consumer<PlayerData> mutator,
            Function<PlayerData, Component> feedback) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlayerData data = PlayerData.get(player);
        mutator.accept(data);
        Net.sync(player);
        ctx.getSource().sendSuccess(() -> feedback.apply(data), false);
        return 1;
    }
}
