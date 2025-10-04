package com.tuempresa.rogue.core.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.CommandDispatcher;
import com.tuempresa.rogue.economy.Economy;
import java.util.Locale;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class RogueShopCommand {
    private static final Map<String, ShopItem> SHOP_ITEMS = Map.of(
        "varita_rapida", new ShopItem("Varita Rápida", 120, Items.BLAZE_ROD),
        "arco_pesado", new ShopItem("Arco Pesado", 120, Items.BOW),
        "armadura_ligera", new ShopItem("Armadura Ligera", 150, Items.LEATHER_CHESTPLATE),
        "armadura_reforzada", new ShopItem("Armadura Reforzada", 150, Items.IRON_CHESTPLATE)
    );

    private RogueShopCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(buildRoot());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildRoot() {
        return Commands.literal("shop")
            .then(Commands.literal("buy")
                .then(Commands.argument("itemId", StringArgumentType.word())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(SHOP_ITEMS.keySet(), builder))
                    .executes(RogueShopCommand::buy)));
    }

    private static int buy(CommandContext<CommandSourceStack> context) {
        ServerPlayer player;
        try {
            player = context.getSource().getPlayerOrException();
        } catch (Exception exception) {
            context.getSource().sendFailure(Component.literal("Solo los jugadores pueden usar este comando."));
            return 0;
        }

        String rawId = StringArgumentType.getString(context, "itemId");
        String itemId = rawId.toLowerCase(Locale.ROOT);
        ShopItem shopItem = SHOP_ITEMS.get(itemId);

        if (shopItem == null) {
            context.getSource().sendFailure(Component.literal("Artículo desconocido: " + rawId));
            return 0;
        }

        if (!Economy.hasGold(player, shopItem.price())) {
            context.getSource().sendFailure(Component.literal("No tienes suficiente oro. Precio: " + shopItem.price()));
            return 0;
        }

        if (!Economy.takeGold(player, shopItem.price())) {
            context.getSource().sendFailure(Component.literal("No se pudo completar la compra."));
            return 0;
        }

        ItemStack stack = shopItem.createStack();
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }

        context.getSource().sendSuccess(
            () -> Component.literal("Has comprado " + shopItem.displayName() + " por " + shopItem.price() + " de oro."),
            false
        );

        return Command.SINGLE_SUCCESS;
    }

    private record ShopItem(String displayName, int price, Item item) {
        private ItemStack createStack() {
            ItemStack stack = new ItemStack(item);
            stack.setHoverName(Component.literal(displayName));
            return stack;
        }
    }
}
