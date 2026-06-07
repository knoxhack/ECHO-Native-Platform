package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeServiceBusSafetyStatus(
        String statusId,
        boolean safeToContinue,
        boolean inMemoryOnly,
        boolean inertHandlesOnly,
        boolean serviceCodeExecuted,
        boolean addonCodeExecuted,
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
