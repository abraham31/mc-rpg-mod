package com.tuempresa.rpgcore.capability;

import com.mojang.serialization.Codec;
import com.tuempresa.rpgcore.ModRpgCore;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.attachment.AttachmentType;

public final class PlayerDataAttachment {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ModRpgCore.MOD_ID, "player_data");

    private static final Codec<PlayerData> CODEC = CompoundTag.CODEC.xmap(PlayerDataAttachment::fromTag, PlayerData::saveNBT);
    private static final StreamCodec<RegistryFriendlyByteBuf, PlayerData> STREAM_CODEC = StreamCodec.of(
        buffer -> {
            CompoundTag tag = ByteBufCodecs.COMPOUND_TAG.decode(buffer);
            return fromTag(tag);
        },
        (buffer, data) -> ByteBufCodecs.COMPOUND_TAG.encode(buffer, data.saveNBT())
    );

    public static final AttachmentType<PlayerData> TYPE = AttachmentType.builder(PlayerData::new)
        .serialize(CODEC, STREAM_CODEC)
        .build();

    private PlayerDataAttachment() {
    }

    private static PlayerData fromTag(CompoundTag tag) {
        PlayerData data = new PlayerData();
        data.loadNBT(tag);
        return data;
    }
}
