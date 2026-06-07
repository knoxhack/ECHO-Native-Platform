package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativePhase13M16SafetyStatus(
        String statusId,
        boolean safeToContinue,
        boolean diagnosticsCaptured,
        boolean supportBundlePlannedOnly,
        boolean commandExecuted,
        boolean processLaunched,
        boolean gameProcessLaunched,
        boolean classloaderCreated,
        boolean gameClassesResolved,
        boolean transformsEnabled,
        boolean transformsPerformed,
        boolean bytecodeMutated,
        boolean registryInjected,
        boolean registryMutated,
        boolean liveNetworkingStarted,
        boolean filesystemMutated,
        List<String> completedChecks
) {
}
