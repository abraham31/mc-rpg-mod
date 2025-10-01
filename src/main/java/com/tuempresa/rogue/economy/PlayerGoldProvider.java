package com.tuempresa.rogue.economy;

import com.tuempresa.rogue.core.RogueMod;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.Capability;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.common.util.LazyOptional;

public class PlayerGoldProvider implements ICapabilityProvider<CompoundTag> {
    public static final ResourceLocation ID = RogueMod.id("player_gold");

    private final PlayerGold backend = new PlayerGold();
    private final LazyOptional<PlayerGold> optional = LazyOptional.of(() -> backend);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
        return capability == RogueCapabilities.PLAYER_GOLD ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return backend.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        backend.deserializeNBT(nbt);
    }

    public void invalidate() {
        optional.invalidate();
    }
}
