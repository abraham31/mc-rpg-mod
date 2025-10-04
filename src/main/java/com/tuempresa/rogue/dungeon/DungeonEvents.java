package com.tuempresa.rogue.dungeon;

import com.tuempresa.rogue.RogueMod;
import com.tuempresa.rogue.config.RogueConfig;
import com.tuempresa.rogue.dungeon.instance.DungeonRun;
import com.tuempresa.rogue.reward.awakening.ArmorAwakening;
import com.tuempresa.rogue.reward.awakening.WeaponAwakening;
import com.tuempresa.rogue.util.Chat;
import com.tuempresa.rogue.util.RogueLogger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;
import java.util.Optional;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod.EventBusSubscriber(modid = RogueMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DungeonEvents {
    private DungeonEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        DungeonManager.onServerTick(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }

        if (entity instanceof Player player) {
            MinecraftServer server = player.getServer();
            if (server != null) {
                DungeonManager.findRunByPlayer(player.getUUID()).ifPresent(run -> run.finishFail(server));
            }
            DungeonManager.cleanupFinishedRuns();
            return;
        }

        if (entity instanceof Mob mob) {
            MinecraftServer server = mob.getServer();
            if (server == null) {
                return;
            }
            Optional<DungeonRun> runOptional = mob.getTags().stream()
                .filter(tag -> tag.startsWith("rogue_run:"))
                .findFirst()
                .flatMap(tag -> {
                    String id = tag.substring("rogue_run:".length());
                    try {
                        UUID runId = UUID.fromString(id);
                        return DungeonManager.getRun(runId);
                    } catch (IllegalArgumentException ignored) {
                        return Optional.empty();
                    }
                });

            runOptional.ifPresent(run -> {
                run.onMobKilled(server, mob.getUUID());
                if (mob.getTags().contains("rogue_mob") && mob.level() instanceof ServerLevel level) {
                    double dropChance = mob.getTags().contains("rogue_boss")
                        ? RogueConfig.awakeningDropChanceBoss()
                        : RogueConfig.awakeningDropChanceCommon();
                    if (level.getRandom().nextDouble() < dropChance) {
                        ExperienceOrb orb = new ExperienceOrb(level, mob.getX(), mob.getY(), mob.getZ(), 1);
                        orb.addTag("awakening");
                        level.addFreshEntity(orb);
                    }
                }
            });
        }
    }

    @SubscribeEvent
    public static void onPickupXp(PlayerXpEvent.PickupXp event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ExperienceOrb orb = event.getOrb();
        if (orb == null || !orb.getTags().contains("awakening")) {
            return;
        }

        MinecraftServer server = serverPlayer.getServer();
        if (server == null) {
            return;
        }

        DungeonManager.findRunByPlayer(serverPlayer.getUUID()).ifPresent(run -> {
            if (run.grantAwakening(serverPlayer)) {
                boolean armorUp = ArmorAwakening.nextLevel(serverPlayer);
                boolean weaponUp = WeaponAwakening.nextLevel(serverPlayer);
                int armorLevel = ArmorAwakening.getLevel(serverPlayer);
                int weaponLevel = WeaponAwakening.getLevel(serverPlayer);
                if (armorUp || weaponUp) {
                    Chat.success(serverPlayer, "¡Tu despertar aumenta! Armadura: " + armorLevel + " | Arma: " + weaponLevel);
                }
                RogueLogger.debug(
                    "Jugador {} sube despertar (arma={}, armadura={})",
                    serverPlayer.getGameProfile().getName(),
                    weaponLevel,
                    armorLevel
                );
            }
        });
    }
}
