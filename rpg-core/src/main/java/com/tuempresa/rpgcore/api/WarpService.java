package com.tuempresa.rpgcore.api;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.tuempresa.rpgcore.util.TeleportUtil;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * API pública para registrar "warps" o destinos rápidos que otros packs pueden
 * reutilizar sin necesidad de escribir lógica de teletransporte. Un warp es un
 * identificador simbólico (por ejemplo {@code prontera/city}) que apunta a una
 * dimensión concreta.
 * <p>
 * Uso recomendado:
 * <pre>
 * WarpService.registerWarp("prontera/city", ResourceKey.create(Registries.DIMENSION,
 *     new ResourceLocation("rpg_content_prontera", "city")));
 * </pre>
 * Posteriormente se puede invocar {@link #teleport(ServerPlayer, String)} para
 * enviar a un jugador al destino registrado.
 */
public final class WarpService {
  private static final Map<String, ResourceKey<Level>> WARPS = new ConcurrentHashMap<>();

  private WarpService() {}

  /**
   * Registra o sustituye un warp.
   *
   * @param warpId identificador del warp (se recomienda el formato
   *     {@code pack/nombre}).
   * @param destination dimensión destino.
   */
  public static void registerWarp(String warpId, ResourceKey<Level> destination) {
    if (warpId == null || warpId.isBlank() || destination == null) {
      return;
    }
    WARPS.put(warpId, destination);
  }

  /**
   * Busca el warp indicado.
   */
  public static Optional<ResourceKey<Level>> getWarp(String warpId) {
    if (warpId == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(WARPS.get(warpId));
  }

  /**
   * Teletransporta al jugador si el warp está registrado y la dimensión existe
   * en el servidor. Devuelve {@code true} si la operación tuvo éxito.
   */
  public static boolean teleport(ServerPlayer player, String warpId) {
    if (player == null) {
      return false;
    }
    Optional<ResourceKey<Level>> warp = getWarp(warpId);
    if (warp.isEmpty()) {
      return false;
    }
    ResourceLocation location = warp.get().location();
    int result = TeleportUtil.tpNamed(player, location.toString());
    return result > 0;
  }
}
