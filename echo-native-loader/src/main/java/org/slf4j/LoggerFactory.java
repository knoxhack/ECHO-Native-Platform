package org.slf4j;

public final class LoggerFactory {
    private static final Logger NOOP_LOGGER = new Logger() {
    };

    private LoggerFactory() {
    }

    public static Logger getLogger(String name) {
        return NOOP_LOGGER;
    }

    public static Logger getLogger(Class<?> type) {
        return NOOP_LOGGER;
    }
}
