package com.tuempresa.rogue.combat;

import com.tuempresa.rogue.core.RogueConstants;
import com.tuempresa.rogue.data.model.Affinity;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class AffinityUtil {
    private static final TagKey<Item> WIND_ITEMS = TagKey.create(Registries.ITEM, RogueConstants.id("affinity/wind"));

    private AffinityUtil() {
    }

    public static Affinity of(ItemStack stack) {
        if (stack.isEmpty()) {
            return Affinity.NONE;
        }
        if (stack.is(WIND_ITEMS)) {
            return Affinity.WIND;
        }
        return Affinity.NONE;
    }
}
