package dev.echo.nativeplatform.contracts;

import java.util.Locale;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public enum EchoNativeRuntimeSide {
    COMMON,
    CLIENT,
    SERVER,
    UNKNOWN;

    public static EchoNativeRuntimeSide from(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "common" -> COMMON;
            case "client" -> CLIENT;
            case "server", "dedicated_server" -> SERVER;
            default -> UNKNOWN;
        };
    }
}
