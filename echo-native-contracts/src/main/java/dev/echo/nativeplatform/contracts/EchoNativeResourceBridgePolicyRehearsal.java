package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeResourceBridgePolicyRehearsal(
        String rehearsalId,
        boolean rehearsed,
        boolean resourceRuntimeAccessed,
        boolean gameClassesResolved,
        boolean classloaderCreated,
        boolean processLaunched,
        boolean commandExecuted,
        boolean filesystemMutated,
        List<String> plannedResourceScopes,
        List<String> blockedCapabilities
) {
}
