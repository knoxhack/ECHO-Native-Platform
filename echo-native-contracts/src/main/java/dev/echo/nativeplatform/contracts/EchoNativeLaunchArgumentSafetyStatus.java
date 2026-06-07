package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeLaunchArgumentSafetyStatus(
        String statusId,
        boolean safeToContinue,
        boolean launchArgumentsPlannedOnly,
        boolean processLaunched,
        boolean gameProcessLaunched,
        boolean commandExecuted,
        boolean classloaderCreated,
        boolean productionClassloader,
        boolean gameClassesResolved,
        boolean libraryDownloadStarted,
        boolean nativeExtractionStarted,
        boolean nativeFilesExtracted,
        boolean registryInjected,
        boolean registryMutated,
        boolean filesystemMutated,
        List<String> completedChecks
) {
}
