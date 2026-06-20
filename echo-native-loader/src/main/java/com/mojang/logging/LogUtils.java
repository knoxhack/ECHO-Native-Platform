package com.mojang.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LogUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger("echo-native-minecraft-logutils");

    private LogUtils() {
    }

    public static Logger getLogger() {
        return LOGGER;
    }
}
