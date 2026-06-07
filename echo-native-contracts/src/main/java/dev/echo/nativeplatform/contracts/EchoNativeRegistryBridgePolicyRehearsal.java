package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeRegistryBridgePolicyRehearsal(
        String rehearsalId,
        boolean rehearsed,
        boolean registryInjected,
        boolean registryMutated,
        boolean gameClassesResolved,
        boolean classloaderCreated,
        boolean processLaunched,
        boolean commandExecuted,
        boolean filesystemMutated,
        List<String> plannedRegistryScopes,
        List<String> blockedCapabilities
) {
}
