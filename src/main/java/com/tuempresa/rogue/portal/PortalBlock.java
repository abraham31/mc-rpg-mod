package com.tuempresa.rogue.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Simple block that delegates interaction handling to {@link PortalSystem}.
 */
public class PortalBlock extends Block {
    private final ResourceLocation portalId;

    public PortalBlock(BlockBehaviour.Properties properties, ResourceLocation portalId) {
        super(properties);
        this.portalId = portalId;
    }

    @Override
    public InteractionResult use(BlockState state,
                                 Level level,
                                 BlockPos pos,
                                 Player player,
                                 InteractionHand hand,
                                 BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        return PortalSystem.get().tryEnter(serverPlayer, portalId.toString());
    }

    public ResourceLocation portalId() {
        return portalId;
    }
}
