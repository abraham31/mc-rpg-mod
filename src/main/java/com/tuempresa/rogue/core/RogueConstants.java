package com.tuempresa.rogue.core;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Valores constantes compartidos a lo largo del mod.
 */
public final class RogueConstants {
    public static final String MOD_ID = "rogue";

    public static final ResourceKey<Level> DIM_CITY1 =
        ResourceKey.create(Registries.DIMENSION, id("city1"));

    public static final ResourceKey<Level> DIM_EARTH =
        ResourceKey.create(Registries.DIMENSION, id("earth_dim"));

    public static final TagKey<EntityType<?>> TAG_ROGUE_MOB =
        TagKey.create(Registries.ENTITY_TYPE, id("rogue_mob"));

    public static final TagKey<EntityType<?>> TAG_EARTH =
        TagKey.create(Registries.ENTITY_TYPE, id("earth"));

    public static final TagKey<Item> TAG_VARITAS =
        TagKey.create(Registries.ITEM, id("varitas"));
    public static final TagKey<Item> TAG_ARCOS =
        TagKey.create(Registries.ITEM, id("arcos"));
    public static final TagKey<Item> TAG_ARMADURAS_LIGERAS =
        TagKey.create(Registries.ITEM, id("armaduras_ligeras"));
    public static final TagKey<Item> TAG_ARMADURAS_REFORZADAS =
        TagKey.create(Registries.ITEM, id("armaduras_reforzadas"));

    private RogueConstants() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
