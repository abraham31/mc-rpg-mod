package com.tuempresa.rpgcore.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.tuempresa.rpgcore.capability.PlayerData;
import com.tuempresa.rpgcore.capability.PlayerDataAttachment;
import com.tuempresa.rpgcore.util.SyncUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class RpgCommands {
  private RpgCommands() {}

  public static void register(RegisterCommandsEvent event) {
    CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
    dispatcher.register(buildRoot());
  }

  private static LiteralArgumentBuilder<CommandSourceStack> buildRoot() {
    return Commands.literal("rpg")
        .requires(src -> src.hasPermission(0))
        .then(Commands.literal("stats")
            .executes(RpgCommands::showStats))
        .then(Commands.literal("xp")
            .requires(src -> src.hasPermission(2))
            .then(Commands.literal("add")
                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                    .executes(RpgCommands::addXp))))
        .then(Commands.literal("level")
            .requires(src -> src.hasPermission(2))
            .then(Commands.literal("set")
                .then(Commands.argument("value", IntegerArgumentType.integer(1))
                    .executes(RpgCommands::setLevel))))
        .then(Commands.literal("money")
            .requires(src -> src.hasPermission(2))
            .then(Commands.literal("add")
                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                    .executes(RpgCommands::addMoney))));
  }

  private static int showStats(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    PlayerData data = player.getData(PlayerDataAttachment.PLAYER_DATA.get());
    String classId = data.getClassId();
    if ("none".equals(classId)) {
      classId = "(sin clase)";
    }
    Component message = Component.literal(String.format("[RPG] Clase: %s | Nivel: %d | XP: %d | Dinero: %d",
        classId, data.getLevel(), data.getXp(), data.getCurrency()));
    ctx.getSource().sendSuccess(() -> message, false);
    return 1;
  }

  private static int addXp(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    long amount = LongArgumentType.getLong(ctx, "amount");
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    PlayerData data = player.getData(PlayerDataAttachment.PLAYER_DATA.get());
    data.addXp(amount);
    SyncUtil.sync(player);
    Component message = Component.literal(String.format(
        "[RPG] XP añadida: +%d. Nivel actual: %d, XP actual: %d",
        amount, data.getLevel(), data.getXp()));
    ctx.getSource().sendSuccess(() -> message, false);
    return 1;
  }

  private static int setLevel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    int level = IntegerArgumentType.getInteger(ctx, "value");
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    PlayerData data = player.getData(PlayerDataAttachment.PLAYER_DATA.get());
    data.setLevel(level);
    SyncUtil.sync(player);
    Component message = Component.literal("[RPG] Nivel establecido: " + data.getLevel());
    ctx.getSource().sendSuccess(() -> message, false);
    return 1;
  }

  private static int addMoney(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
    long amount = LongArgumentType.getLong(ctx, "amount");
    ServerPlayer player = ctx.getSource().getPlayerOrException();
    PlayerData data = player.getData(PlayerDataAttachment.PLAYER_DATA.get());
    data.addCurrency(amount);
    SyncUtil.sync(player);
    Component message = Component.literal(String.format("[RPG] Dinero añadido: +%d. Total: %d", amount, data.getCurrency()));
    ctx.getSource().sendSuccess(() -> message, false);
    return 1;
  }
}
