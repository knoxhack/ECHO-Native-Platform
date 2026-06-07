package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativePhase13M17ArtifactReadiness(
        String readinessId,
        boolean phase13M17ArtifactReady,
        boolean safeForIsolatedLaunchAttempt,
        boolean localArtifactsReady,
        boolean libraryDownloadStarted,
        boolean nativeExtractionStarted,
        boolean processLaunched,
        boolean commandExecuted,
        boolean classloaderCreated,
        boolean gameClassesResolved,
        boolean filesystemMutated,
        List<String> completedChecks
) {
}
