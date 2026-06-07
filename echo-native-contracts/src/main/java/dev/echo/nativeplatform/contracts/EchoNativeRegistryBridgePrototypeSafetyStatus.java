package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeRegistryBridgePrototypeSafetyStatus(
        String statusId,
        boolean safeToContinue,
        boolean localOnly,
        boolean sandboxOnly,
        boolean minecraftRegistryTouched,
        boolean registryInjected,
        boolean registryMutated,
        boolean classloaderCreated,
        boolean gameClassesResolved,
        boolean processLaunched,
        boolean commandExecuted,
        boolean filesystemMutated,
        List<String> completedChecks
) {
}
