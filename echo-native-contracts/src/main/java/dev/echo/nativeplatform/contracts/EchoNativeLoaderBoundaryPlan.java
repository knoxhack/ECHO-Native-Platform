package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeLoaderBoundaryPlan(
        String boundaryId,
        String summary,
        boolean productionClassloaderAllowed,
        boolean launchAllowed,
        List<String> inputs,
        List<String> outputs,
        List<String> blockedCapabilities
) {
}
