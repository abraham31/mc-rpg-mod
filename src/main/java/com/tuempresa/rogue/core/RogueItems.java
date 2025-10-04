package com.tuempresa.rogue.core;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registro centralizado de ítems del mod.
 */
public final class RogueItems {
    private RogueItems() {
    }

    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(Registries.ITEM, RogueConstants.MOD_ID);

    public static final DeferredHolder<Item, Item> VARITA_RAPIDA = ITEMS.register(
        "varita_rapida",
        () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> ARCO_PESADO = ITEMS.register(
        "arco_pesado",
        () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> ARMADURA_LIGERA = ITEMS.register(
        "armadura_ligera",
        () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> ARMADURA_REFORZADA = ITEMS.register(
        "armadura_reforzada",
        () -> new Item(new Item.Properties()));

    public static void init() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modBus);
    }
}
