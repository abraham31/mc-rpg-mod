package com.tuempresa.rogue.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class Chat {
    private Chat() {
    }

    public static void tip(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message));
    }

    public static void success(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal("§a" + message));
    }

    public static void error(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal("§c" + message));
    }
}
