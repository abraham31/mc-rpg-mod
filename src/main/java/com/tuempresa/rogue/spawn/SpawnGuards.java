package com.tuempresa.rogue.spawn;

import com.tuempresa.rogue.config.RogueConfig;
import com.tuempresa.rogue.core.RogueConstants;
import com.tuempresa.rogue.core.RogueMod;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

@Mod.EventBusSubscriber(modid = RogueMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SpawnGuards {
    private SpawnGuards() {
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.MobSpawn event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL) {
            return;
        }
        if (event.getLevel().dimension() != RogueConstants.DIM_EARTH) {
            return;
        }
        Mob mob = event.getEntity();
        if (!mob.getType().is(RogueConstants.TAG_ROGUE_MOB)) {
            if (RogueConfig.logVerbose()) {
                RogueMod.LOGGER.debug("Bloqueado spawn de {} en {}", mob.getType(), event.getLevel().dimension().location());
            }
            event.setResult(Event.Result.DENY);
        }
    }
}
