package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeCrashBoundarySimulationResult(
        String simulationId,
        boolean simulated,
        boolean crashBoundaryActive,
        boolean capturedDiagnostics,
        boolean classloaderCreated,
        boolean processLaunched,
        boolean terminatedProcess,
        boolean mutatedState,
        List<String> simulatedFailureCases
) {
}
