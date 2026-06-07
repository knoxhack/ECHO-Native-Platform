package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeTransformSafetyStatus(
        String statusId,
        boolean safeToContinue,
        boolean localOnly,
        boolean descriptorOnly,
        boolean transformPlanningOnly,
        boolean transformsEnabled,
        boolean minecraftBytecodeTransformed,
        boolean addonBytecodeTransformed,
        boolean bytecodeMutated,
        boolean classloaderCreated,
        boolean gameClassesResolved,
        boolean processLaunched,
        boolean commandExecuted,
        boolean liveNetworkingStarted,
        boolean registryInjected,
        boolean registryMutated,
        boolean filesystemMutated,
        List<String> completedChecks
) {
}
