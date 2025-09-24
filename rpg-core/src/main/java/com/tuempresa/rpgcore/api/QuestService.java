package com.tuempresa.rpgcore.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.tuempresa.rpgcore.ModRpgCore;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Servicio de misiones que expone hooks básicos de progreso. Los packs pueden
 * registrar listeners para reaccionar a eventos clave sin suscribirse
 * manualmente al {@link NeoForge#EVENT_BUS}.
 */
public final class QuestService {
  /** Callback para misiones tipo KILL. */
  @FunctionalInterface
  public interface KillHandler {
    void onKill(ServerPlayer player, EntityType<?> victimType);
  }

  /** Callback para misiones tipo COLLECT. */
  @FunctionalInterface
  public interface CollectHandler {
    void onCollect(ServerPlayer player, ItemStack stack);
  }

  /** Callback para misiones tipo REACH. */
  @FunctionalInterface
  public interface ReachHandler {
    void onReach(ServerPlayer player, Level level);
  }

  /** Callback para misiones tipo INTERACT. */
  @FunctionalInterface
  public interface InteractHandler {
    void onInteract(ServerPlayer player, InteractionHand hand, InteractionResult result, Entity target);
  }

  private static final List<KillHandler> KILL_HANDLERS = new CopyOnWriteArrayList<>();
  private static final List<CollectHandler> COLLECT_HANDLERS = new CopyOnWriteArrayList<>();
  private static final List<ReachHandler> REACH_HANDLERS = new CopyOnWriteArrayList<>();
  private static final List<InteractHandler> INTERACT_HANDLERS = new CopyOnWriteArrayList<>();

  private static final QuestService INSTANCE = new QuestService();

  private QuestService() {}

  /** Inicializa el servicio registrando los listeners globales. */
  public static void register() {
    NeoForge.EVENT_BUS.register(INSTANCE);
    ModRpgCore.LOG.debug("QuestService inicializado");
  }

  /** Añade un listener para eventos de asesinato. */
  public static void addKillHandler(KillHandler handler) {
    if (handler != null) {
      KILL_HANDLERS.add(handler);
    }
  }

  /** Añade un listener para recogida de objetos. */
  public static void addCollectHandler(CollectHandler handler) {
    if (handler != null) {
      COLLECT_HANDLERS.add(handler);
    }
  }

  /** Añade un listener para llegada a un nivel/dimensión. */
  public static void addReachHandler(ReachHandler handler) {
    if (handler != null) {
      REACH_HANDLERS.add(handler);
    }
  }

  /** Añade un listener para interacciones de jugador. */
  public static void addInteractHandler(InteractHandler handler) {
    if (handler != null) {
      INTERACT_HANDLERS.add(handler);
    }
  }

  @SubscribeEvent
  public void onLivingDeath(LivingDeathEvent event) {
    if (event.getEntity().level().isClientSide()) {
      return;
    }
    Entity source = event.getSource().getEntity();
    if (source instanceof ServerPlayer player) {
      for (KillHandler handler : KILL_HANDLERS) {
        handler.onKill(player, event.getEntity().getType());
      }
    }
  }

  // TODO 1.21: reactivar COLLECT cuando el evento de pickup esté disponible
  /*
  @SubscribeEvent
  public void onItemPickup(net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    ItemStack stack = event.getItemEntity().getItem();
    for (CollectHandler handler : COLLECT_HANDLERS) {
      handler.onCollect(player, stack.copy());
    }
  }
  */

  @SubscribeEvent
  public void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    Level level = player.server.getLevel(event.getTo());
    if (level == null) {
      return;
    }
    for (ReachHandler handler : REACH_HANDLERS) {
      handler.onReach(player, level);
    }
  }

  @SubscribeEvent
  public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    InteractionHand hand = event.getHand();
    InteractionResult result = event.isCanceled() ? InteractionResult.FAIL : InteractionResult.PASS;
    Entity target = event.getTarget();
    for (InteractHandler handler : INTERACT_HANDLERS) {
      handler.onInteract(player, hand, result, target);
    }
  }

  @SubscribeEvent
  public void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
    if (!(event.getEntity() instanceof ServerPlayer player)) {
      return;
    }
    InteractionHand hand = event.getHand();
    InteractionResult result = event.isCanceled() ? InteractionResult.FAIL : InteractionResult.PASS;
    Entity target = event.getTarget();
    for (InteractHandler handler : INTERACT_HANDLERS) {
      handler.onInteract(player, hand, result, target);
    }
  }
}
