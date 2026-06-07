package dev.echo.nativeplatform.contracts;

import java.util.Locale;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public enum EchoNativeTrustLevel {
    OFFICIAL,
    PARTNER,
    COMMUNITY,
    LOCAL,
    UNTRUSTED,
    UNKNOWN;

    public static EchoNativeTrustLevel from(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "official" -> OFFICIAL;
            case "partner" -> PARTNER;
            case "community" -> COMMUNITY;
            case "local" -> LOCAL;
            case "untrusted", "blocked" -> UNTRUSTED;
            default -> UNKNOWN;
        };
    }
}
