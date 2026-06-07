package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeLifecycleStubSafetyStatus(
        String statusId,
        boolean safeToContinue,
        boolean stubOnly,
        boolean inertHandlersOnly,
        boolean realAddonCodeExecuted,
        boolean minecraftClassesResolved,
        boolean classloaderCreated,
        boolean registryBridgeTouched,
        boolean transformsPerformed,
        boolean processLaunched,
        boolean filesystemMutated,
        List<String> completedChecks
) {
}
