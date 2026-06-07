package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeAddonRuntimeDiscoverySafetyStatus(
        String statusId,
        boolean safeToContinue,
        boolean dataOnly,
        boolean addonCodeExecuted,
        boolean classloaderCreated,
        boolean resolvesRuntimeClasses,
        boolean gameProcessLaunched,
        boolean minecraftLaunched,
        boolean commandExecuted,
        boolean registryInjected,
        boolean registryMutated,
        boolean filesystemMutated,
        List<String> completedChecks
) {
}
