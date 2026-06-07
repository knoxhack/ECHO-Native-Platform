package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeMinecraftResolverSafetyStatus(
        String statusId,
        boolean safeToContinue,
        boolean minecraftResolverStarted,
        boolean remoteManifestDownloaded,
        boolean libraryDownloadStarted,
        boolean nativeExtractionStarted,
        boolean classloaderCreated,
        boolean gameClassesResolved,
        boolean processLaunched,
        boolean commandExecuted,
        boolean filesystemMutated,
        List<String> completedChecks
) {
}
