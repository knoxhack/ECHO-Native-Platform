package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativePhase13PrototypeSafetyGate(
        String gateId,
        boolean passed,
        boolean minecraftResolverStarted,
        boolean libraryDownloadStarted,
        boolean nativeExtractionStarted,
        boolean classloaderCreated,
        boolean gameClassesResolved,
        boolean processLaunched,
        boolean commandExecuted,
        boolean registryInjected,
        boolean registryMutated,
        boolean filesystemMutated,
        List<String> safetyGates
) {
}
