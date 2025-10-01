package com.tuempresa.rogue.core;

import com.tuempresa.rogue.combat.AffinityUtil;
import com.tuempresa.rogue.config.RogueConfig;
import com.tuempresa.rogue.core.RogueConstants;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.living.LivingHurtEvent;

public final class RogueServerEvents {
    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(RogueMod.DUNGEON_DATA);
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        if (AffinityUtil.of(player.getMainHandItem()) != com.tuempresa.rogue.data.model.Affinity.WIND) {
            return;
        }
        if (!target.getType().is(RogueConstants.TAG_EARTH)) {
            return;
        }
        float bonus = (float) RogueConfig.affinityBonusMultiplier();
        event.setAmount(event.getAmount() * (1.0F + bonus));
    }
}
