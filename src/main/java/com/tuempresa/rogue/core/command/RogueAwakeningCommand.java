package com.tuempresa.rogue.core.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.tuempresa.rogue.reward.awakening.ArmorAwakening;
import com.tuempresa.rogue.reward.awakening.WeaponAwakening;
import com.tuempresa.rogue.util.Chat;
import com.tuempresa.rogue.util.RogueLogger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Locale;

public final class RogueAwakeningCommand {
    private static final SimpleCommandExceptionType UNKNOWN_TYPE = new SimpleCommandExceptionType(
        Component.literal("Tipo de despertar desconocido. Usa arma, armadura o ambos.")
    );
    private static final List<String> TYPES = List.of("arma", "armadura", "ambos");

    private RogueAwakeningCommand() {
    }

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("awakening")
            .then(Commands.literal("set")
                .then(Commands.argument("jugador", EntityArgument.player())
                    .then(Commands.argument("tipo", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(TYPES, builder))
                        .then(Commands.argument("nivel", IntegerArgumentType.integer(0))
                            .executes(ctx -> setLevel(
                                ctx.getSource(),
                                EntityArgument.getPlayer(ctx, "jugador"),
                                StringArgumentType.getString(ctx, "tipo"),
                                IntegerArgumentType.getInteger(ctx, "nivel")
                            )))))));
    }

    private static int setLevel(CommandSourceStack source, ServerPlayer player, String type, int level)
        throws CommandSyntaxException {
        String normalized = type.toLowerCase(Locale.ROOT);
        int weaponLevel = WeaponAwakening.getLevel(player);
        int armorLevel = ArmorAwakening.getLevel(player);
        switch (normalized) {
            case "arma" -> weaponLevel = WeaponAwakening.setLevel(player, level);
            case "armadura" -> armorLevel = ArmorAwakening.setLevel(player, level);
            case "ambos" -> {
                weaponLevel = WeaponAwakening.setLevel(player, level);
                armorLevel = ArmorAwakening.setLevel(player, level);
            }
            default -> throw UNKNOWN_TYPE.create();
        }
        source.sendSuccess(
            () -> Component.literal(
                "Niveles de despertar de " + player.getName().getString() + ": arma=" + weaponLevel + ", armadura=" + armorLevel
            ),
            true
        );
        if (source.getEntity() != player) {
            Chat.tip(player, "Tus niveles de despertar ahora son arma=" + weaponLevel + " y armadura=" + armorLevel + ".");
        }
        RogueLogger.debug(
            "Comando QA ajusta despertar de {} (arma={}, armadura={})",
            player.getGameProfile().getName(),
            weaponLevel,
            armorLevel
        );
        return Math.max(weaponLevel, armorLevel);
    }
}
