package dev.echo.nativeplatform.contracts;

import java.util.Locale;

@EchoNativeApiStatus(value = EchoNativeApiStability.STABLE, since = "0.1.0-native-beta")
public enum EchoNativeApiStability {
    STABLE,
    BETA,
    INTERNAL,
    TEST_ONLY,
    ALPHA,
    EXPERIMENTAL,
    DEPRECATED,
    UNKNOWN;

    public static EchoNativeApiStability from(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "stable" -> STABLE;
            case "beta" -> BETA;
            case "internal" -> INTERNAL;
            case "test_only", "test-only", "testonly" -> TEST_ONLY;
            case "alpha" -> ALPHA;
            case "experimental" -> EXPERIMENTAL;
            case "deprecated" -> DEPRECATED;
            default -> UNKNOWN;
        };
    }
}
