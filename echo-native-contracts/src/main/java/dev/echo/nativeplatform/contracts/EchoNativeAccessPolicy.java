package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public record EchoNativeAccessPolicy(
        boolean dryRunOnly,
        boolean launchBlocked,
        boolean transformsBlocked,
        boolean registryInjectionBlocked,
        List<String> blockedCapabilities
) {
    public static EchoNativeAccessPolicy nativeDryRun() {
        return new EchoNativeAccessPolicy(
                true,
                true,
                true,
                true,
                List.of("minecraft.launch", "bytecode.transforms", "registry.injection", "native.library.extraction")
        );
    }

    @Deprecated(since = "0.1.0", forRemoval = false)
    public static EchoNativeAccessPolicy phase12() {
        return nativeDryRun();
    }
}
