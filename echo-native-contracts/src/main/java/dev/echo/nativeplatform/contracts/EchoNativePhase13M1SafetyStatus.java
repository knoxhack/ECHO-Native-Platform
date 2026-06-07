package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativePhase13M1SafetyStatus(
        String statusId,
        boolean safeToContinue,
        boolean commandExecuted,
        boolean processLaunched,
        boolean gameProcessLaunched,
        boolean classloaderCreated,
        boolean resolvesRuntimeClasses,
        boolean filesystemMutated,
        boolean unsafeRuntimeWorkStarted,
        List<String> completedM1Checks
) {
}
