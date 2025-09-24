package com.tuempresa.rpgcore.capability;

import com.tuempresa.rpgcore.util.SyncUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class PlayerDataEvents {
  private static final String NBT_KEY = "rpg_core_playerdata";

  private PlayerDataEvents() {}

  public static void registerOnGameBus() {
    NeoForge.EVENT_BUS.register(new PlayerDataEvents());
  }

  @SubscribeEvent
  public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      CompoundTag persistentData = player.getPersistentData();
      if (persistentData.contains(NBT_KEY)) {
        PlayerData playerData = player.getData(PlayerDataAttachment.PLAYER_DATA.get());
        playerData.loadNBT(persistentData.getCompound(NBT_KEY));
      }
      SyncUtil.sync(player);
    }
  }

  @SubscribeEvent
  public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      saveToPersistentData(player);
    }
  }

  @SubscribeEvent
  public void onClone(PlayerEvent.Clone event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      saveToPersistentData(player);
      SyncUtil.sync(player);
    }
  }

  private static void saveToPersistentData(ServerPlayer player) {
    PlayerData playerData = player.getData(PlayerDataAttachment.PLAYER_DATA.get());
    CompoundTag persistentData = player.getPersistentData();
    persistentData.put(NBT_KEY, playerData.saveNBT());
  }
}
