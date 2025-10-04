package com.tuempresa.rogue.util;

import com.mojang.logging.LogUtils;
import com.tuempresa.rogue.config.RogueConfig;
import org.slf4j.Logger;

/**
 * Encapsula el logger global del mod y expone métodos de conveniencia.
 */
public final class RogueLogger {
    private static final Logger LOGGER = LogUtils.getLogger();

    private RogueLogger() {
    }

    public static Logger raw() {
        return LOGGER;
    }

    public static void info(String message, Object... args) {
        LOGGER.info(message, args);
    }

    public static void warn(String message, Object... args) {
        LOGGER.warn(message, args);
    }

    public static void error(String message, Object... args) {
        LOGGER.error(message, args);
    }

    public static void debug(String message, Object... args) {
        if (RogueConfig.logVerbose()) {
            LOGGER.debug(message, args);
        }
    }
}
