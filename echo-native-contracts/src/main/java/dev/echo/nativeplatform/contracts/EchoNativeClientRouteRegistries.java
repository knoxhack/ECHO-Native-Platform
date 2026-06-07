package dev.echo.nativeplatform.contracts;

import java.util.ServiceLoader;
import java.util.ServiceConfigurationError;

public final class EchoNativeClientRouteRegistries {
    private static volatile EchoNativeClientRouteRegistry discovered;

    private EchoNativeClientRouteRegistries() {
    }

    public static EchoNativeClientRouteRegistry get() {
        EchoNativeClientRouteRegistry registry = discovered;
        if (registry != null && registry != EchoNativeClientRouteRegistry.NOOP) {
            return registry;
        }
        registry = discover(Thread.currentThread().getContextClassLoader());
        if (registry == EchoNativeClientRouteRegistry.NOOP) {
            registry = discover(EchoNativeClientRouteRegistry.class.getClassLoader());
        }
        if (registry == EchoNativeClientRouteRegistry.NOOP
                && EchoNativeClientRuntimeEnvironment.isNativeLoaderActive()) {
            throw missingNativeRouteProvider();
        }
        if (registry != EchoNativeClientRouteRegistry.NOOP || discovered == null) {
            discovered = registry;
        }
        return registry;
    }

    public static boolean available() {
        return get() != EchoNativeClientRouteRegistry.NOOP;
    }

    public static void resetDiscoveryForRuntimeReload() {
        discovered = null;
    }

    private static EchoNativeClientRouteRegistry discover(ClassLoader classLoader) {
        if (classLoader == null) {
            return EchoNativeClientRouteRegistry.NOOP;
        }
        try {
            for (EchoNativeClientRouteRegistry registry : ServiceLoader.load(EchoNativeClientRouteRegistry.class, classLoader)) {
                if (registry != null) {
                    return registry;
                }
            }
        } catch (RuntimeException | ServiceConfigurationError ignored) {
            return EchoNativeClientRouteRegistry.NOOP;
        }
        return EchoNativeClientRouteRegistry.NOOP;
    }

    private static IllegalStateException missingNativeRouteProvider() {
        String marker = EchoNativeClientRuntimeEnvironment.activeMarker();
        return new IllegalStateException(
                "Native Loader client route registry provider is missing while native client mode is active"
                        + (marker.isBlank() ? "" : " (" + marker + ")")
                        + ". Ensure echo-native-loader is on the Native Loader client runtime classpath and "
                        + "META-INF/services/" + EchoNativeClientRouteRegistry.class.getName()
                        + " points at dev.echo.nativeplatform.loader.NativeLoaderClientRouteRegistryProvider."
        );
    }
}
