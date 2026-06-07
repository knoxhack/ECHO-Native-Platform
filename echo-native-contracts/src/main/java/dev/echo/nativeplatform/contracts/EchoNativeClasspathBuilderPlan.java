package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeClasspathBuilderPlan(
        String planId,
        boolean planningOnly,
        boolean classpathBuilderStarted,
        boolean classpathEntriesPlannedOnly,
        boolean classloaderCreated,
        boolean productionClassloader,
        boolean gameClassesResolved,
        boolean libraryDownloadStarted,
        boolean nativeExtractionStarted,
        boolean processLaunched,
        boolean commandExecuted,
        boolean registryInjected,
        boolean registryMutated,
        boolean filesystemMutated,
        List<Map<String, Object>> plannedEntries
) {
}
