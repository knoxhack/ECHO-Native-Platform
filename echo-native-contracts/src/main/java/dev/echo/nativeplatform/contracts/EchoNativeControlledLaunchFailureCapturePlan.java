package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeControlledLaunchFailureCapturePlan(
        String planId,
        boolean planned,
        boolean diagnosticsCaptured,
        boolean supportBundlePlannedOnly,
        boolean commandExecuted,
        boolean processLaunched,
        boolean gameProcessLaunched,
        boolean filesystemMutated,
        List<String> plannedSignals
) {
}
