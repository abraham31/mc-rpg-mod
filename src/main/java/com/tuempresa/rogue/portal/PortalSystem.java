package com.tuempresa.rogue.portal;

import com.tuempresa.rogue.RogueMod;
import com.tuempresa.rogue.data.model.PortalDef;
import com.tuempresa.rogue.dungeon.DungeonInstance;
import com.tuempresa.rogue.dungeon.DungeonManager;
import com.tuempresa.rogue.economy.Economy;
import com.tuempresa.rogue.world.RogueDimensions;
import com.tuempresa.rogue.world.TeleportUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

import java.util.Map;
import java.util.Optional;

/**
 * Centralises the interaction logic for dungeon portals. It validates portal
 * definitions, checks player requirements, deducts gold and finally
 * teleports the player to the configured dungeon dimension.
 */
public final class PortalSystem {
    private static final PortalSystem INSTANCE = new PortalSystem();

    private PortalSystem() {
    }

    public static PortalSystem get() {
        return INSTANCE;
    }

    public InteractionResult activatePortal(ServerPlayer player, ResourceLocation portalId) {
        PortalDef portalDef = findPortal(portalId);
        if (portalDef == null) {
            player.sendSystemMessage(Component.literal("Este portal no está configurado."));
            return InteractionResult.FAIL;
        }

        int playerLevel = player.experienceLevel;
        if (playerLevel < portalDef.requiredLevel()) {
            player.sendSystemMessage(Component.literal(
                "Necesitas nivel " + portalDef.requiredLevel() + " para entrar en " + portalDef.displayName() + "."));
            return InteractionResult.FAIL;
        }

        int entryCost = portalDef.activationCost();
        if (entryCost > 0 && !Economy.hasGold(player, entryCost)) {
            player.sendSystemMessage(Component.literal(
                "Necesitas " + entryCost + " de oro para activar este portal."));
            return InteractionResult.FAIL;
        }

        if (entryCost > 0 && !Economy.takeGold(player, entryCost)) {
            player.sendSystemMessage(Component.literal("No se pudo descontar el oro necesario."));
            return InteractionResult.FAIL;
        }

        DungeonInstance instance = DungeonManager.createOrJoin(player, portalDef);
        RogueMod.LOGGER.debug("{} se unió a la instancia {} del portal {}", player.getGameProfile().getName(),
            instance.dungeonId(), portalDef.id());

        MinecraftServer server = player.serverLevel().getServer();
        Optional<ServerLevel> level = TeleportUtil.level(server, RogueDimensions.EARTH_DUNGEON_LEVEL);
        if (level.isEmpty()) {
            player.sendSystemMessage(Component.literal("La dimensión del portal no está disponible."));
            return InteractionResult.FAIL;
        }

        BlockPos spawn = level.get().getSharedSpawnPos();
        TeleportUtil.teleport(player, RogueDimensions.EARTH_DUNGEON_LEVEL, spawn);
        player.sendSystemMessage(Component.literal("Has activado el portal hacia " + portalDef.displayName() + "."));
        return InteractionResult.SUCCESS;
    }

    private PortalDef findPortal(ResourceLocation portalId) {
        Map<ResourceLocation, PortalDef> portals = RogueMod.DUNGEON_DATA.portals();
        return portals.get(portalId);
    }
}
