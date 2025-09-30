package com.tuempresa.rogue;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Entry point for the Rogue mod. Responsible for bootstrapping shared services
 * and exposing the central logger used throughout the project.
 */
@Mod(RogueMod.MOD_ID)
public final class RogueMod {
    public static final String MOD_ID = "rogue";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RogueMod() {
        LOGGER.info("Inicializando el mod {}", MOD_ID);
    }
}
