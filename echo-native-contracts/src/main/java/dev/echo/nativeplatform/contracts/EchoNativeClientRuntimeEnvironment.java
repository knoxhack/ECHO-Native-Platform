package dev.echo.nativeplatform.contracts;

import java.util.Locale;

/**
 * Shared Native Loader client-mode detection for first-party client surfaces.
 */
public final class EchoNativeClientRuntimeEnvironment {
    public static final String NATIVE_LOADER_PROPERTY = "echo.native.loader";
    public static final String WINDOWED_CLIENT_PROPERTY = "echo.native.windowedClient";
    public static final String RUNTIME_MODE_PROPERTY = "echo.native.runtime.mode";
    public static final String NATIVE_CLIENT_ENV = "ECHO_NATIVE_CLIENT";
    public static final String NATIVE_LOADER_ENV = "ECHO_NATIVE_LOADER";
    public static final String WINDOWED_CLIENT_ENV = "ECHO_NATIVE_WINDOWED_CLIENT";
    public static final String RUNTIME_MODE_ENV = "ECHO_NATIVE_RUNTIME_MODE";
    public static final String WINDOWED_NATIVE_CLIENT_MODE = "windowed-native-client";
    public static final String NATIVE_CLIENT_MODE = "native-client";

    private EchoNativeClientRuntimeEnvironment() {
    }

    public static boolean isNativeLoaderActive() {
        return truthySystemProperty(NATIVE_LOADER_PROPERTY)
                || truthyEnvironment(NATIVE_CLIENT_ENV)
                || truthyEnvironment(NATIVE_LOADER_ENV)
                || isWindowedNativeClient()
                || NATIVE_CLIENT_MODE.equals(runtimeMode());
    }

    public static boolean isWindowedNativeClient() {
        return truthySystemProperty(WINDOWED_CLIENT_PROPERTY)
                || truthyEnvironment(WINDOWED_CLIENT_ENV)
                || WINDOWED_NATIVE_CLIENT_MODE.equals(runtimeMode());
    }

    public static String runtimeMode() {
        String value = normalizedSystemProperty(RUNTIME_MODE_PROPERTY);
        if (value.isBlank()) {
            value = normalizedEnvironment(RUNTIME_MODE_ENV);
        }
        return value.replace('_', '-');
    }

    public static EchoNativeRuntimeMode runtimeLane() {
        String mode = runtimeMode();
        if (WINDOWED_NATIVE_CLIENT_MODE.equals(mode) || NATIVE_CLIENT_MODE.equals(mode) || isNativeLoaderActive()) {
            return EchoNativeRuntimeMode.NATIVE_CLIENT;
        }
        if ("standalone".equals(mode) || "standalone-native".equals(mode)) {
            return EchoNativeRuntimeMode.STANDALONE;
        }
        return EchoNativeRuntimeMode.NEOFORGE;
    }

    public static String activeMarker() {
        if (truthySystemProperty(NATIVE_LOADER_PROPERTY)) {
            return NATIVE_LOADER_PROPERTY;
        }
        if (truthySystemProperty(WINDOWED_CLIENT_PROPERTY)) {
            return WINDOWED_CLIENT_PROPERTY;
        }
        if (truthyEnvironment(NATIVE_CLIENT_ENV)) {
            return NATIVE_CLIENT_ENV;
        }
        if (truthyEnvironment(NATIVE_LOADER_ENV)) {
            return NATIVE_LOADER_ENV;
        }
        if (truthyEnvironment(WINDOWED_CLIENT_ENV)) {
            return WINDOWED_CLIENT_ENV;
        }
        if (WINDOWED_NATIVE_CLIENT_MODE.equals(runtimeMode())) {
            return RUNTIME_MODE_PROPERTY + "=" + WINDOWED_NATIVE_CLIENT_MODE;
        }
        if (NATIVE_CLIENT_MODE.equals(runtimeMode())) {
            return RUNTIME_MODE_PROPERTY + "=" + NATIVE_CLIENT_MODE;
        }
        return "";
    }

    private static boolean truthySystemProperty(String key) {
        String value = normalizedSystemProperty(key);
        return "true".equals(value) || "1".equals(value) || "yes".equals(value) || "on".equals(value);
    }

    private static boolean truthyEnvironment(String key) {
        String value = normalizedEnvironment(key);
        return "true".equals(value) || "1".equals(value) || "yes".equals(value) || "on".equals(value);
    }

    private static String normalizedSystemProperty(String key) {
        return System.getProperty(key, "").trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizedEnvironment(String key) {
        String value = System.getenv(key);
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
