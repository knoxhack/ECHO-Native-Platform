package dev.echo.nativeplatform.contracts;

import java.util.Map;

/**
 * Resolves the active runtime lane from system properties, environment, or explicit configuration.
 *
 * <p>Rules:</p>
 * <ul>
 *   <li>{@code echo.native.runtime.lane} system property is authoritative</li>
 *   <li>{@code ECHO_NATIVE_RUNTIME_LANE} environment variable is second</li>
 *   <li>Default is {@link EchoNativeRuntimeLane#NATIVE_LOADER}</li>
 *   <li>If Native Loader cannot be proven available, falls back to NeoForge</li>
 *   <li>Standalone is selected only when explicitly requested or when no Minecraft client is present</li>
 * </ul>
 */
public final class EchoNativeRuntimeLaneResolver {
    public static final String PROPERTY_KEY = "echo.native.runtime.lane";
    public static final String ENV_KEY = "ECHO_NATIVE_RUNTIME_LANE";

    private final EchoNativeRuntimeLane explicitLane;
    private final boolean nativeLoaderAvailable;
    private final boolean neoForgeAvailable;
    private final boolean standaloneAvailable;

    public EchoNativeRuntimeLaneResolver() {
        this(readExplicitLane(), detectNativeLoader(), detectNeoForge(), true);
    }

    public EchoNativeRuntimeLaneResolver(
            EchoNativeRuntimeLane explicitLane,
            boolean nativeLoaderAvailable,
            boolean neoForgeAvailable,
            boolean standaloneAvailable
    ) {
        this.explicitLane = explicitLane;
        this.nativeLoaderAvailable = nativeLoaderAvailable;
        this.neoForgeAvailable = neoForgeAvailable;
        this.standaloneAvailable = standaloneAvailable;
    }

    /**
     * Returns the active runtime lane after applying fallback rules.
     */
    public EchoNativeRuntimeLane resolve() {
        if (explicitLane != null && explicitLane != EchoNativeRuntimeLane.UNKNOWN) {
            if (explicitLane == EchoNativeRuntimeLane.NATIVE_LOADER && !nativeLoaderAvailable) {
                return fallbackFromNativeLoader();
            }
            if (explicitLane == EchoNativeRuntimeLane.NEOFORGE && !neoForgeAvailable) {
                return fallbackFromNeoForge();
            }
            if (explicitLane == EchoNativeRuntimeLane.STANDALONE && !standaloneAvailable) {
                return fallbackFromStandalone();
            }
            return explicitLane;
        }
        if (nativeLoaderAvailable) {
            return EchoNativeRuntimeLane.NATIVE_LOADER;
        }
        if (neoForgeAvailable) {
            return EchoNativeRuntimeLane.NEOFORGE;
        }
        if (standaloneAvailable) {
            return EchoNativeRuntimeLane.STANDALONE;
        }
        return EchoNativeRuntimeLane.UNKNOWN;
    }

    /**
     * Returns the primary lane without fallback. This is the lane the platform 
     * wants to use, regardless of availability.
     */
    public EchoNativeRuntimeLane primaryLane() {
        if (explicitLane != null && explicitLane != EchoNativeRuntimeLane.UNKNOWN) {
            return explicitLane;
        }
        return EchoNativeRuntimeLane.NATIVE_LOADER;
    }

    public Map<String, Object> toReport() {
        EchoNativeRuntimeLane active = resolve();
        return Map.of(
                "explicitLane", explicitLane == null ? "" : explicitLane.laneId(),
                "nativeLoaderAvailable", nativeLoaderAvailable,
                "neoForgeAvailable", neoForgeAvailable,
                "standaloneAvailable", standaloneAvailable,
                "activeLane", active.laneId(),
                "activeDisplayName", active.displayName(),
                "isPrimaryNative", active.isNative(),
                "isFallback", active.isFallback(),
                "isHarness", active.isHarness()
        );
    }

    private static EchoNativeRuntimeLane readExplicitLane() {
        String property = System.getProperty(PROPERTY_KEY, "").trim();
        if (!property.isBlank()) {
            return EchoNativeRuntimeLane.fromId(property);
        }
        String env = System.getenv().getOrDefault(ENV_KEY, "").trim();
        if (!env.isBlank()) {
            return EchoNativeRuntimeLane.fromId(env);
        }
        return null;
    }

    private static boolean detectNativeLoader() {
        try {
            Class.forName("dev.echo.nativeplatform.loader.EchoNativeModuleLoader", false,
                    EchoNativeRuntimeLaneResolver.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static boolean detectNeoForge() {
        try {
            Class.forName("net.neoforged.fml.common.Mod", false,
                    EchoNativeRuntimeLaneResolver.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private EchoNativeRuntimeLane fallbackFromNativeLoader() {
        if (neoForgeAvailable) {
            return EchoNativeRuntimeLane.NEOFORGE;
        }
        if (standaloneAvailable) {
            return EchoNativeRuntimeLane.STANDALONE;
        }
        return EchoNativeRuntimeLane.UNKNOWN;
    }

    private EchoNativeRuntimeLane fallbackFromNeoForge() {
        if (standaloneAvailable) {
            return EchoNativeRuntimeLane.STANDALONE;
        }
        return EchoNativeRuntimeLane.UNKNOWN;
    }

    private EchoNativeRuntimeLane fallbackFromStandalone() {
        if (nativeLoaderAvailable) {
            return EchoNativeRuntimeLane.NATIVE_LOADER;
        }
        if (neoForgeAvailable) {
            return EchoNativeRuntimeLane.NEOFORGE;
        }
        return EchoNativeRuntimeLane.UNKNOWN;
    }
}
