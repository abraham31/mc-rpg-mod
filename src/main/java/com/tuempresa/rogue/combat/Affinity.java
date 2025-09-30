package com.tuempresa.rogue.combat;

import com.tuempresa.rogue.RogueMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

/**
 * Representa las afinidades elementales disponibles dentro del sistema de combate.
 * Cada afinidad mantiene referencias a los tags de ítems y entidades que determinan
 * su pertenencia.
 */
public enum Affinity {
    EARTH("earth"),
    FIRE("fire"),
    WIND("wind"),
    WATER("water"),
    NONE(null);

    private static final Affinity[] SEARCHABLE = Arrays.stream(values())
        .filter(affinity -> affinity.itemTag != null)
        .toArray(Affinity[]::new);

    private final TagKey<Item> itemTag;
    private final TagKey<EntityType<?>> entityTypeTag;

    Affinity(String name) {
        if (name == null) {
            this.itemTag = null;
            this.entityTypeTag = null;
        } else {
            ResourceLocation itemTagId = RogueMod.id("affinity/" + name);
            this.itemTag = TagKey.create(Registries.ITEM, itemTagId);

            ResourceLocation entityTagId = RogueMod.id("affinity/" + name);
            this.entityTypeTag = TagKey.create(Registries.ENTITY_TYPE, entityTagId);
        }
    }

    public TagKey<EntityType<?>> entityTypeTag() {
        return entityTypeTag;
    }

    public TagKey<Item> itemTag() {
        return itemTag;
    }

    /**
     * Determina la afinidad del {@link ItemStack} proporcionado evaluando los tags configurados.
     *
     * @param stack arma o herramienta que se desea evaluar.
     * @return la afinidad correspondiente, o {@link #NONE} si no posee tags de afinidad.
     */
    public static Affinity of(ItemStack stack) {
        if (stack.isEmpty()) {
            return NONE;
        }

        for (Affinity affinity : SEARCHABLE) {
            if (stack.is(affinity.itemTag)) {
                return affinity;
            }
        }
        return NONE;
    }
}
