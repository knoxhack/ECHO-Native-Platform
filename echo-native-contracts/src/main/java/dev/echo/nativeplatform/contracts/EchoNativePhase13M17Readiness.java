package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativePhase13M17Readiness(
        String readinessId,
        boolean phase13M17Ready,
        boolean launchPreflightComplete,
        boolean safeForIsolatedLaunchAttempt,
        boolean commandExecuted,
        boolean processLaunched,
        boolean gameProcessLaunched,
        boolean classloaderCreated,
        boolean gameClassesResolved,
        boolean libraryDownloadStarted,
        boolean nativeExtractionStarted,
        boolean transformsEnabled,
        boolean registryInjected,
        boolean filesystemMutated,
        List<String> completedChecks
) {
}
