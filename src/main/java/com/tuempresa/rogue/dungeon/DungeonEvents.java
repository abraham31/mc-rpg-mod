package com.tuempresa.rogue.dungeon;

import com.tuempresa.rogue.core.RogueMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
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
            mob.getTags().stream()
                .filter(tag -> tag.startsWith("rogue_run:"))
                .findFirst()
                .ifPresent(tag -> {
                    String id = tag.substring("rogue_run:".length());
                    try {
                        UUID runId = UUID.fromString(id);
                        DungeonManager.getRun(runId).ifPresent(run -> run.onMobKilled(server, mob.getUUID()));
                    } catch (IllegalArgumentException ignored) {
                    }
                });
        }
    }
}
