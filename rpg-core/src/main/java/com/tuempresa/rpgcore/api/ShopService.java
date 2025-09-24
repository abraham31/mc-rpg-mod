package com.tuempresa.rpgcore.api;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.ResourceLocation;

/**
 * Servicio de registro de tiendas. Los packs de contenido pueden declarar las
 * ofertas disponibles y el core se encarga de exponerlas para menús o NPCs sin
 * requerir clases adicionales.
 * <p>
 * Ejemplo rápido:
 * <pre>
 * ShopService.registerShop("prontera/armas", List.of(
 *     new ShopService.ShopEntry(new ResourceLocation("minecraft", "iron_sword"), 150)
 * ));
 * </pre>
 */
public final class ShopService {
  private static final Map<String, List<ShopEntry>> SHOPS = new ConcurrentHashMap<>();

  private ShopService() {}

  /**
   * Describe un producto a la venta.
   *
   * @param itemId identificador del ítem.
   * @param price coste en la moneda interna del RPG.
   */
  public record ShopEntry(ResourceLocation itemId, long price) {}

  /**
   * Registra una tienda. Un registro posterior con el mismo {@code shopId}
   * sustituirá al existente.
   */
  public static void registerShop(String shopId, List<ShopEntry> entries) {
    if (shopId == null || shopId.isBlank()) {
      return;
    }
    List<ShopEntry> safeList = entries == null
        ? List.of()
        : List.copyOf(entries);
    SHOPS.put(shopId, safeList);
  }

  /**
   * Obtiene las entradas de una tienda. Si no existe, devuelve una lista vacía.
   */
  public static List<ShopEntry> getShop(String shopId) {
    if (shopId == null) {
      return List.of();
    }
    return SHOPS.getOrDefault(shopId, List.of());
  }

  /**
   * Devuelve un conjunto inmutable con los IDs de tienda registrados.
   */
  public static Collection<String> getRegisteredShops() {
    return Collections.unmodifiableSet(SHOPS.keySet());
  }
}
