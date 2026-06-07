package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeNativeExtractionSafetyStatus(
        String statusId,
        boolean safeToContinue,
        boolean nativeExtractionStarted,
        boolean nativeExtractionAllowed,
        boolean nativeFilesExtracted,
        boolean libraryDownloadStarted,
        boolean classloaderCreated,
        boolean productionClassloader,
        boolean gameClassesResolved,
        boolean processLaunched,
        boolean commandExecuted,
        boolean registryInjected,
        boolean registryMutated,
        boolean filesystemMutated,
        List<String> completedChecks
) {
}
