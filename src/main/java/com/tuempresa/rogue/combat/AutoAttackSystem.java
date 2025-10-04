package com.tuempresa.rogue.combat;

import com.tuempresa.rogue.RogueMod;
import com.tuempresa.rogue.core.RogueConstants;
import com.tuempresa.rogue.data.model.ItemConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Gestiona los autoataques de armas especiales configuradas vía data packs.
 */
public final class AutoAttackSystem {
    private static final String TAG_AUTO_ATTACK = "RogueAutoAttack";
    private static final String TAG_BASE_DAMAGE = "RogueAutoAttackBaseDamage";
    private static final String PLAYER_AWAKENINGS_KEY = "rogue_awakenings_active";

    private static final Map<UUID, AttackTracker> ATTACK_TRACKERS = new HashMap<>();

    private AutoAttackSystem() {
    }

    public static void tick(MinecraftServer server) {
        if (server == null) {
            return;
        }

        Set<UUID> seen = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            seen.add(player.getUUID());
            handlePlayer(player);
        }

        ATTACK_TRACKERS.keySet().removeIf(id -> !seen.contains(id));
    }

    public static void handleImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        Level level = projectile.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        CompoundTag tag = projectile.getPersistentData();
        if (!tag.getBoolean(TAG_AUTO_ATTACK)) {
            return;
        }

        event.setCanceled(true);
        projectile.discard();

        if (!(event.getRayTraceResult() instanceof EntityHitResult entityHit)) {
            return;
        }

        Entity hitEntity = entityHit.getEntity();
        if (!(hitEntity instanceof LivingEntity living)) {
            return;
        }

        float baseDamage = tag.getFloat(TAG_BASE_DAMAGE);
        if (baseDamage <= 0.0F) {
            return;
        }

        Entity owner = projectile.getOwner();
        DamageSource source;
        float totalDamage = baseDamage;
        if (owner instanceof ServerPlayer player) {
            int awakenings = Math.max(1, player.getPersistentData().getInt(PLAYER_AWAKENINGS_KEY));
            totalDamage = baseDamage * awakenings;
            source = serverLevel.damageSources().playerAttack(player);
        } else {
            source = serverLevel.damageSources().magic();
        }

        living.hurt(source, totalDamage);
    }

    private static void handlePlayer(ServerPlayer player) {
        if (player.isSpectator() || player.isDeadOrDying()) {
            ATTACK_TRACKERS.remove(player.getUUID());
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || (!stack.is(RogueConstants.TAG_VARITAS) && !stack.is(RogueConstants.TAG_ARCOS))) {
            ATTACK_TRACKERS.remove(player.getUUID());
            return;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Optional<ItemConfig> configOpt = RogueMod.ITEM_CONFIGS.get(itemId);
        if (configOpt.isEmpty()) {
            ATTACK_TRACKERS.remove(player.getUUID());
            return;
        }

        ItemConfig config = configOpt.get();
        AttackTracker tracker = ATTACK_TRACKERS.computeIfAbsent(player.getUUID(), id -> new AttackTracker());
        tracker.updateItem(itemId);
        if (tracker.shouldAttack(config.attackIntervalTicks())) {
            fireProjectile(player, stack, config.baseDamage());
        }
    }

    private static void fireProjectile(ServerPlayer player, ItemStack stack, float baseDamage) {
        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        Projectile projectile;
        if (stack.is(RogueConstants.TAG_ARCOS)) {
            projectile = createArrow(serverLevel, player);
        } else {
            projectile = createSnowball(serverLevel, player);
        }

        markProjectile(projectile, baseDamage);
        serverLevel.addFreshEntity(projectile);
    }

    private static Projectile createArrow(ServerLevel level, ServerPlayer player) {
        Arrow arrow = new Arrow(level, player);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.setBaseDamage(0.0D);
        arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.0F, 1.0F);
        return arrow;
    }

    private static Projectile createSnowball(ServerLevel level, ServerPlayer player) {
        Snowball snowball = new Snowball(level, player);
        snowball.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 0.5F);
        return snowball;
    }

    private static void markProjectile(Projectile projectile, float baseDamage) {
        CompoundTag tag = projectile.getPersistentData();
        tag.putBoolean(TAG_AUTO_ATTACK, true);
        tag.putFloat(TAG_BASE_DAMAGE, baseDamage);
    }

    private static final class AttackTracker {
        private ResourceLocation currentItem;
        private int cooldown;

        void updateItem(ResourceLocation item) {
            if (!item.equals(currentItem)) {
                currentItem = item;
                cooldown = 0;
            }
        }

        boolean shouldAttack(int interval) {
            if (cooldown > 0) {
                cooldown--;
                return false;
            }

            cooldown = Math.max(1, interval);
            return true;
        }
    }
}
