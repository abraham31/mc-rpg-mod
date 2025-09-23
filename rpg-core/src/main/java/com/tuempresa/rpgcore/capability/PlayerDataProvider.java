package com.tuempresa.rpgcore.capability;

import java.util.UUID;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import com.tuempresa.rpgcore.ModRpgCore;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.Capability;
import net.neoforged.neoforge.capabilities.CapabilityManager;
import net.neoforged.neoforge.capabilities.CapabilityToken;
import net.neoforged.neoforge.capabilities.ICapabilitySerializable;
import net.neoforged.neoforge.common.util.LazyOptional;

public final class PlayerDataProvider implements ICapabilitySerializable<CompoundTag> {
    private static final Marker MARKER = MarkerManager.getMarker("PlayerDataProvider");

    public static final ResourceLocation ID = new ResourceLocation(ModRpgCore.MOD_ID, "player_data");
    public static final Capability<IPlayerData> PLAYER_DATA = CapabilityManager.get(new CapabilityToken<>() {});

    private final PlayerData data;
    private final LazyOptional<IPlayerData> optional;
    private final UUID playerId;
    private final String playerName;

    public PlayerDataProvider(Player player) {
        this.data = new PlayerData();
        this.optional = LazyOptional.of(() -> data);
        this.playerId = player.getUUID();
        this.playerName = player.getGameProfile().getName();
        ModRpgCore.LOG.debug(MARKER, "Creating provider for {} ({})", playerName, playerId);
    }

    public void invalidate() {
        optional.invalidate();
    }

    public CompoundTag serializeNBT() {
        ModRpgCore.LOG.debug(MARKER, "Saving data for {} ({})", playerName, playerId);
        return data.saveNBT();
    }

    public void deserializeNBT(CompoundTag nbt) {
        ModRpgCore.LOG.debug(MARKER, "Loading data for {} ({})", playerName, playerId);
        data.loadNBT(nbt);
    }

    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return serializeNBT();
    }

    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        deserializeNBT(nbt);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap) {
        if (cap == PLAYER_DATA) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return getCapability(cap);
    }

    public PlayerData getData() {
        return data;
    }
}
