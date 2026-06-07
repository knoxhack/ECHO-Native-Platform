package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeClassloaderBoundaryRehearsal(
        String rehearsalId,
        boolean rehearsed,
        boolean classloaderCreated,
        boolean productionClassloader,
        boolean resolvesRuntimeClasses,
        boolean processLaunched,
        List<String> rehearsedBoundaries,
        List<String> blockedCapabilities
) {
}
