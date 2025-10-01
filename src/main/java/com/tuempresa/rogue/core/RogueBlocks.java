package com.tuempresa.rogue.core;

import com.tuempresa.rogue.portal.PortalBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Registro centralizado de bloques del mod.
 */
public final class RogueBlocks {
    private RogueBlocks() {
    }

    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(Registries.BLOCK, RogueConstants.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(Registries.ITEM, RogueConstants.MOD_ID);

    public static final DeferredHolder<Block, PortalBlock> PORTAL_TIERRA = BLOCKS.register(
        "portal_tierra",
        () -> new PortalBlock(
            BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_BLOCK)
                .lightLevel(state -> 7),
            RogueConstants.id("portal_tierra")));

    public static final DeferredHolder<Item, BlockItem> PORTAL_TIERRA_ITEM = ITEMS.register(
        "portal_tierra",
        () -> new BlockItem(PORTAL_TIERRA.get(), new Item.Properties()));

    public static void init() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }
}
