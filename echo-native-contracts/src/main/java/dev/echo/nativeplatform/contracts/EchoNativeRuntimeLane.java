package dev.echo.nativeplatform.contracts;

/**
 * Canonical runtime lane identifiers. No other lane names are valid.
 */
@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public enum EchoNativeRuntimeLane {
    NATIVE_LOADER("native_loader", "Native Loader", 1, true),
    NEOFORGE("neoforge", "NeoForge", 2, false),
    STANDALONE("standalone", "Standalone Runtime", 3, true),
    UNKNOWN("unknown", "Unknown", Integer.MAX_VALUE, false);

    private final String id;
    private final String displayName;
    private final int priority;
    private final boolean canBePrimary;

    EchoNativeRuntimeLane(String id, String displayName, int priority, boolean canBePrimary) {
        this.id = id;
        this.displayName = displayName;
        this.priority = priority;
        this.canBePrimary = canBePrimary;
    }

    public String laneId() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public int priority() {
        return priority;
    }

    public boolean canBePrimary() {
        return canBePrimary;
    }

    public boolean isNative() {
        return this == NATIVE_LOADER;
    }

    public boolean isFallback() {
        return this == NEOFORGE;
    }

    public boolean isHarness() {
        return this == STANDALONE;
    }

    public static EchoNativeRuntimeLane fromId(String id) {
        if (id == null || id.isBlank()) {
            return UNKNOWN;
        }
        String normalized = id.trim().toLowerCase();
        for (EchoNativeRuntimeLane lane : values()) {
            if (lane.id.equals(normalized)) {
                return lane;
            }
        }
        if (normalized.contains("forge") || normalized.contains("neoforge")) {
            return NEOFORGE;
        }
        if (normalized.contains("standalone") || normalized.contains("harness")) {
            return STANDALONE;
        }
        if (normalized.contains("native")) {
            return NATIVE_LOADER;
        }
        return UNKNOWN;
    }

    public static EchoNativeRuntimeLane preferred() {
        return NATIVE_LOADER;
    }
}
