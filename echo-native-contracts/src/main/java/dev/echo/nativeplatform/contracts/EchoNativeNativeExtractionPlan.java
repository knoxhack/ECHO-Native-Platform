package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeNativeExtractionPlan(
        String planId,
        boolean planningOnly,
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
        List<Map<String, Object>> plannedNativeEntries
) {
}
