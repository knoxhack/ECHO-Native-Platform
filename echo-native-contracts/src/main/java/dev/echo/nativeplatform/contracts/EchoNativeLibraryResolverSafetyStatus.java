package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeLibraryResolverSafetyStatus(
        String statusId,
        boolean safeToContinue,
        boolean libraryResolverStarted,
        boolean libraryDownloadStarted,
        boolean remoteManifestDownloaded,
        boolean cacheMutated,
        boolean nativeExtractionStarted,
        boolean classloaderCreated,
        boolean gameClassesResolved,
        boolean processLaunched,
        boolean commandExecuted,
        boolean registryInjected,
        boolean registryMutated,
        boolean filesystemMutated,
        List<String> completedChecks
) {
}
