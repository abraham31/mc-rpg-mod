package com.tuempresa.rpgcore.capability;

import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class PlayerDataAttachment {
  public static final String MODID = "rpg_core";

  public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
      DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);

  public static final Supplier<AttachmentType<PlayerData>> PLAYER_DATA =
      ATTACHMENTS.register("player_data", () ->
          AttachmentType.builder(PlayerData::new)
              // Persistencia NBT (serializer simple con CompoundTag)
              .serialize(AttachmentType.Serializer.of(
                  (pd, ops) -> pd.saveNBT(),           // encode -> CompoundTag
                  (tag, ops) -> { PlayerData pd = new PlayerData(); pd.loadNBT(tag); return pd; } // decode
              ))
              .copyOnDeath() // clona al respawn
              .build()
      );

  public static void register(IEventBus modBus) {
    ATTACHMENTS.register(modBus);
  }
}
