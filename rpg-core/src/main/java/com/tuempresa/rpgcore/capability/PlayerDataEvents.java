package com.tuempresa.rpgcore.capability;

import com.tuempresa.rpgcore.api.WarpService;
import com.tuempresa.rpgcore.util.SyncUtil;

import java.util.Optional;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class PlayerDataEvents {
  private static final String NBT_KEY = "rpg_core_playerdata";
  private static final String AUTO_WARP_KEY = "rpg_core_auto_warp";

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

      if (!persistentData.getBoolean(AUTO_WARP_KEY)) {
        Optional<ResourceKey<Level>> warp = WarpService.getWarp("prontera/city");
        if (warp.isEmpty()) {
          player.sendSystemMessage(Component.literal(
              "[RPG] El pack Prontera no está instalado. Añade rpg-content-prontera a mods/ o datapacks."));
          return;
        }

        boolean teleported = WarpService.teleport(player, "prontera/city");
        if (teleported) {
          persistentData.putBoolean(AUTO_WARP_KEY, true);
          player.sendSystemMessage(Component.literal("[RPG] Bienvenido a Prontera."));
        } else {
          player.sendSystemMessage(Component.literal(
              "[RPG] No se pudo cargar la dimensión de Prontera. Crea un mundo nuevo tras instalar el pack."));
        }
      }
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
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }

    AttachmentType<PlayerData> type = PlayerDataAttachment.PLAYER_DATA.get();
    PlayerData oldData = event.getOriginal().getData(type);
    PlayerData newData = player.getData(type);
    newData.loadNBT(oldData.saveNBT());
    saveToPersistentData(player);
    SyncUtil.sync(player);
  }

  private static void saveToPersistentData(ServerPlayer player) {
    PlayerData playerData = player.getData(PlayerDataAttachment.PLAYER_DATA.get());
    CompoundTag persistentData = player.getPersistentData();
    persistentData.put(NBT_KEY, playerData.saveNBT());
  }
}
