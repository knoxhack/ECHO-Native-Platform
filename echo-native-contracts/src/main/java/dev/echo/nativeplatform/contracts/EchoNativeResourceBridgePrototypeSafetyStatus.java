package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeResourceBridgePrototypeSafetyStatus(
        String statusId,
        boolean safeToContinue,
        boolean localOnly,
        boolean descriptorOnly,
        boolean resourceRuntimeAccessed,
        boolean minecraftResourceManagerTouched,
        boolean classloaderCreated,
        boolean gameClassesResolved,
        boolean processLaunched,
        boolean commandExecuted,
        boolean filesystemMutated,
        boolean registryInjected,
        boolean registryMutated,
        List<String> completedChecks
) {
}
