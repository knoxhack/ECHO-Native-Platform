package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeCrashBoundaryPlan(
        String boundaryId,
        boolean capturesDiagnostics,
        boolean terminatesProcess,
        boolean mutatesState,
        List<String> boundaries,
        List<String> recoveryReports
) {
}
