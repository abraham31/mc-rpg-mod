package com.tuempresa.rogue.combat;

import com.tuempresa.rogue.RogueMod;
import com.tuempresa.rogue.core.RogueConstants;
import com.tuempresa.rogue.data.model.ItemConfig;
import com.tuempresa.rogue.reward.awakening.WeaponAwakening;
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
import net.minecraft.world.phys.Vec3;
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
    private static final String TAG_WEAPON_LEVEL = "RogueAutoAttackWeaponLevel";
    private static final String TAG_PIERCE_REMAINING = "RogueAutoAttackPierce";

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

        boolean shouldDiscard = true;

        if (!(event.getRayTraceResult() instanceof EntityHitResult entityHit)) {
            if (shouldDiscard) {
                projectile.discard();
            }
            return;
        }

        Entity hitEntity = entityHit.getEntity();
        if (!(hitEntity instanceof LivingEntity living)) {
            projectile.discard();
            return;
        }

        float baseDamage = tag.getFloat(TAG_BASE_DAMAGE);
        if (baseDamage <= 0.0F) {
            projectile.discard();
            return;
        }

        Entity owner = projectile.getOwner();
        DamageSource source = owner instanceof ServerPlayer player
            ? serverLevel.damageSources().playerAttack(player)
            : serverLevel.damageSources().magic();

        living.hurt(source, baseDamage);

        if (projectile instanceof AbstractArrow && tag.contains(TAG_PIERCE_REMAINING)) {
            int remaining = tag.getInt(TAG_PIERCE_REMAINING);
            if (remaining > 0) {
                remaining--;
                if (remaining > 0) {
                    tag.putInt(TAG_PIERCE_REMAINING, remaining);
                } else {
                    tag.remove(TAG_PIERCE_REMAINING);
                }
                shouldDiscard = false;
                Vec3 movement = projectile.getDeltaMovement();
                if (movement.lengthSqr() > 1.0E-4D) {
                    Vec3 offset = movement.normalize().scale(0.3D);
                    projectile.setPos(
                        projectile.getX() + offset.x,
                        projectile.getY() + offset.y,
                        projectile.getZ() + offset.z
                    );
                }
            }
        }

        if (shouldDiscard) {
            projectile.discard();
        }
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
        int awakeningLevel = WeaponAwakening.getLevel(player);
        AttackTracker tracker = ATTACK_TRACKERS.computeIfAbsent(player.getUUID(), id -> new AttackTracker());
        tracker.updateItem(itemId);
        int interval = config.attackInterval(awakeningLevel);
        if (tracker.shouldAttack(interval)) {
            float damage = config.baseDamage() * config.damageMultiplier(awakeningLevel);
            fireProjectile(player, stack, damage, awakeningLevel);
        }
    }

    private static void fireProjectile(ServerPlayer player, ItemStack stack, float baseDamage, int awakeningLevel) {
        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (stack.is(RogueConstants.TAG_ARCOS)) {
            Projectile projectile = createArrow(serverLevel, player, awakeningLevel);
            int pierce = awakeningLevel >= 3 ? 1 : 0;
            markProjectile(projectile, baseDamage, awakeningLevel, pierce);
            serverLevel.addFreshEntity(projectile);
            return;
        }

        if (awakeningLevel >= 3) {
            Projectile left = createSnowball(serverLevel, player, -7.5F);
            Projectile right = createSnowball(serverLevel, player, 7.5F);
            markProjectile(left, baseDamage, awakeningLevel, 0);
            markProjectile(right, baseDamage, awakeningLevel, 0);
            serverLevel.addFreshEntity(left);
            serverLevel.addFreshEntity(right);
        } else {
            Projectile projectile = createSnowball(serverLevel, player, 0.0F);
            markProjectile(projectile, baseDamage, awakeningLevel, 0);
            serverLevel.addFreshEntity(projectile);
        }
    }

    private static Projectile createArrow(ServerLevel level, ServerPlayer player, int awakeningLevel) {
        Arrow arrow = new Arrow(level, player);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.setBaseDamage(0.0D);
        arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.0F, 1.0F);
        if (awakeningLevel >= 3) {
            arrow.setPierceLevel((byte) Math.max(1, arrow.getPierceLevel()));
        }
        return arrow;
    }

    private static Projectile createSnowball(ServerLevel level, ServerPlayer player, float yawOffset) {
        Snowball snowball = new Snowball(level, player);
        snowball.shootFromRotation(player, player.getXRot(), player.getYRot() + yawOffset, 0.0F, 1.5F, 0.5F);
        return snowball;
    }

    private static void markProjectile(Projectile projectile, float baseDamage, int awakeningLevel, int pierce) {
        CompoundTag tag = projectile.getPersistentData();
        tag.putBoolean(TAG_AUTO_ATTACK, true);
        tag.putFloat(TAG_BASE_DAMAGE, baseDamage);
        tag.putInt(TAG_WEAPON_LEVEL, awakeningLevel);
        if (pierce > 0) {
            tag.putInt(TAG_PIERCE_REMAINING, pierce);
        } else {
            tag.remove(TAG_PIERCE_REMAINING);
        }
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
