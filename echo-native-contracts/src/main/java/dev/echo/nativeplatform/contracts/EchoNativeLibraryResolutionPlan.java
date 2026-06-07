package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeLibraryResolutionPlan(
        String planId,
        boolean planningOnly,
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
        List<Map<String, Object>> plannedLibraries,
        List<String> missingLibraries
) {
}
