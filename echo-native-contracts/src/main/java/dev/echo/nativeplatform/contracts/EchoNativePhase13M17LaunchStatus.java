package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativePhase13M17LaunchStatus(
        String statusId,
        boolean phase13M17AttemptComplete,
        boolean controlledFailure,
        boolean localArtifactsReady,
        boolean launchAttempted,
        boolean processLaunched,
        boolean gameProcessLaunched,
        boolean mainMenuReached,
        boolean commandExecuted,
        boolean classloaderCreated,
        boolean gameClassesResolved,
        boolean libraryDownloadStarted,
        boolean nativeExtractionStarted,
        boolean registryInjected,
        boolean transformsEnabled,
        boolean filesystemMutated,
        List<String> completedChecks
) {
}
