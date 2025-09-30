package com.tuempresa.rogue;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.tuempresa.rogue.data.DungeonDataException;
import com.tuempresa.rogue.data.DungeonDataReloader;
import com.tuempresa.rogue.economy.Economy;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Handles Forge level events such as command registration and data reload listeners.
 */
@Mod.EventBusSubscriber(modid = RogueMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RogueEvents {
    private RogueEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(DungeonDataReloader.getInstance());
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal(RogueMod.MOD_ID)
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("reload")
                .executes(context -> reloadDungeonData(context.getSource())))
            .then(Commands.literal("gold")
                .then(Commands.literal("add")
                    .then(Commands.argument("jugador", EntityArgument.player())
                        .then(Commands.argument("cantidad", IntegerArgumentType.integer(0))
                            .executes(context -> addGold(
                                context.getSource(),
                                EntityArgument.getPlayer(context, "jugador"),
                                IntegerArgumentType.getInteger(context, "cantidad"))))))
                .then(Commands.literal("get")
                    .then(Commands.argument("jugador", EntityArgument.player())
                        .executes(context -> getGold(
                            context.getSource(),
                            EntityArgument.getPlayer(context, "jugador")))))));
    }

    private static int reloadDungeonData(CommandSourceStack source) {
        DungeonDataReloader reloader = DungeonDataReloader.getInstance();
        try {
            reloader.reloadNow(source.getServer());
            source.sendSuccess(() -> Component.literal(
                "Se recargaron " + reloader.dungeons().size() + " mazmorras y " + reloader.portals().size() + " portales."),
                true);
            return 1;
        } catch (DungeonDataException e) {
            RogueMod.LOGGER.error("Error recargando datos de mazmorras", e);
            source.sendFailure(Component.literal("Error al recargar mazmorras: " + e.getMessage()));
            return 0;
        }
    }

    private static int addGold(CommandSourceStack source, ServerPlayer target, int amount) {
        int newTotal = Economy.giveGold(target, amount);
        source.sendSuccess(() -> Component.literal(
            "Se añadieron " + amount + " de oro a " + target.getDisplayName().getString() + ". Total: " + newTotal),
            true);
        if (source.getEntity() != target) {
            target.sendSystemMessage(Component.literal(
                "Has recibido " + amount + " de oro. Ahora tienes " + newTotal + "."));
        }
        return newTotal;
    }

    private static int getGold(CommandSourceStack source, ServerPlayer target) {
        int total = Economy.getGold(target);
        source.sendSuccess(() -> Component.literal(
            target.getDisplayName().getString() + " tiene " + total + " de oro."),
            false);
        return total;
    }
}
