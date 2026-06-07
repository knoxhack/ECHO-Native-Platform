package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeCrashBoundaryResult(
        String boundaryId,
        boolean crashBoundaryActive,
        boolean capturedDiagnostics,
        boolean terminatedProcess,
        boolean mutatedState,
        long simulatedCrashCount,
        List<String> protectedPhases
) {
}
