package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeClassloaderBoundaryPlan(
        String planId,
        boolean classloaderCreated,
        boolean productionClassloader,
        boolean resolvesRuntimeClasses,
        List<String> plannedBoundaries,
        List<String> blockedCapabilities
) {
}
