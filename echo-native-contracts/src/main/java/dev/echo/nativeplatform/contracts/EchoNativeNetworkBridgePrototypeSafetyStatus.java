package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeNetworkBridgePrototypeSafetyStatus(
        String statusId,
        boolean safeToContinue,
        boolean localOnly,
        boolean descriptorOnly,
        boolean liveNetworkingStarted,
        boolean socketOpened,
        boolean clientConnectionOpened,
        boolean serverConnectionOpened,
        boolean packetSent,
        boolean packetReceived,
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
