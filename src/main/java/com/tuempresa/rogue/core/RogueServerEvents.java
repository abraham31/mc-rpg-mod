package com.tuempresa.rogue.core;

import com.tuempresa.rogue.combat.AffinityUtil;
import com.tuempresa.rogue.config.RogueConfig;
import com.tuempresa.rogue.portal.PortalBlock;
import com.tuempresa.rogue.util.RogueLogger;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.living.LivingHurtEvent;

public final class RogueServerEvents {
    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(RogueMod.DUNGEON_DATA);
    }

    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!level.dimension().equals(RogueConstants.DIM_CITY1)) {
            return;
        }
        ensureCityPortal(level);
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

    private void ensureCityPortal(ServerLevel level) {
        BlockPos portalPos = RogueConfig.cityPortalPos();
        level.setDefaultSpawnPos(portalPos.above(), 0.0F);

        BlockState baseState = Blocks.SMOOTH_STONE.defaultBlockState();
        BlockPos basePos = portalPos.below();
        boolean baseUpdated = false;
        if (!level.getBlockState(basePos).is(baseState.getBlock())) {
            level.setBlockAndUpdate(basePos, baseState);
            baseUpdated = true;
        }

        PortalBlock portalBlock = RogueBlocks.PORTAL_TIERRA.get();
        BlockState portalState = portalBlock.defaultBlockState();
        boolean portalUpdated = false;
        if (!level.getBlockState(portalPos).is(portalState.getBlock())) {
            level.setBlockAndUpdate(portalPos, portalState);
            portalUpdated = true;
        }

        if (!portalBlock.portalId().equals(RogueConfig.cityPortalDungeon())) {
            RogueLogger.warn("El portal de ciudad apunta a {} pero la configuración especifica {}.",
                portalBlock.portalId(), RogueConfig.cityPortalDungeon());
        } else if (baseUpdated || portalUpdated) {
            RogueLogger.info("Portal de ciudad asegurado en {} con destino {}.", portalPos, portalBlock.portalId());
        }
    }
}
