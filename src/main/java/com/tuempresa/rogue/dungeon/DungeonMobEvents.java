package com.tuempresa.rogue.dungeon;

import com.tuempresa.rogue.RogueMod;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.UUID;

/**
 * Maneja los eventos de vida/muerte de los mobs pertenecientes a una partida
 * de mazmorra para actualizar el progreso de las salas.
 */
@Mod.EventBusSubscriber(modid = RogueMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
final class DungeonMobEvents {
    private static final String TAG_RUN_ID = "RogueRunId";

    private DungeonMobEvents() {
    }

    @SubscribeEvent
    public static void onMobDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        if (!mob.getTags().contains("rogue_mob")) {
            return;
        }

        if (!mob.getPersistentData().contains(TAG_RUN_ID)) {
            return;
        }

        UUID runId = mob.getPersistentData().getUUID(TAG_RUN_ID);
        DungeonManager.onMobDefeated(runId, mob.getUUID());
    }
}
