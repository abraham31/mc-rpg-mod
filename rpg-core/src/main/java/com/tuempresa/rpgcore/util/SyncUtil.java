package com.tuempresa.rpgcore.util;

import com.tuempresa.rpgcore.capability.PlayerDataAttachment;
import com.tuempresa.rpgcore.capability.PlayerData;
import com.tuempresa.rpgcore.net.Net;
import com.tuempresa.rpgcore.net.S2CPlayerData;
import net.minecraft.server.level.ServerPlayer;

public final class SyncUtil {
  private SyncUtil() {}

  public static void sync(ServerPlayer sp) {
    PlayerData pd = sp.getData(PlayerDataAttachment.PLAYER_DATA.get());
    Net.sendTo(sp, new S2CPlayerData(pd.getClassId(), pd.getLevel(), pd.getXp(), pd.getCurrency()));
  }
}
