package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeLifecycleSimulationResult(
        String simulationId,
        boolean simulated,
        boolean executedAddonCode,
        boolean classloaderCreated,
        boolean processLaunched,
        List<String> completedPhases,
        List<String> simulatedModules
) {
}
