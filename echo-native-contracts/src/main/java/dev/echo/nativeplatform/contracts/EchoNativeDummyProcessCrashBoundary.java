package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeDummyProcessCrashBoundary(
        String boundaryId,
        boolean verified,
        boolean crashContained,
        boolean timeoutContained,
        boolean nonZeroExitContained,
        boolean stdoutCaptured,
        boolean stderrCaptured,
        boolean gameProcessLaunched,
        boolean classloaderCreated,
        boolean filesystemMutated,
        List<String> containedFailureModes
) {
}
