package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeControlledTestProcessPreflight(
        String preflightId,
        boolean ready,
        boolean commandExecuted,
        boolean processLaunched,
        boolean gameProcessLaunched,
        boolean filesystemMutated,
        List<String> plannedTargets,
        List<String> blockedTargets
) {
}
