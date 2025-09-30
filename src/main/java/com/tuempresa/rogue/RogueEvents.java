package com.tuempresa.rogue;

import com.mojang.brigadier.CommandDispatcher;
import com.tuempresa.rogue.data.DungeonDataException;
import com.tuempresa.rogue.data.DungeonDataReloader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
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
                .executes(context -> reloadDungeonData(context.getSource()))));
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
}
