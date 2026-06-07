package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativePhase13BridgeSafetyStatus(
        String statusId,
        boolean safeToContinue,
        boolean resourceRuntimeAccessed,
        boolean registryInjected,
        boolean registryMutated,
        boolean gameClassesResolved,
        boolean classloaderCreated,
        boolean processLaunched,
        boolean commandExecuted,
        boolean filesystemMutated,
        boolean unsafeRuntimeWorkStarted,
        List<String> completedBridgeChecks
) {
}
