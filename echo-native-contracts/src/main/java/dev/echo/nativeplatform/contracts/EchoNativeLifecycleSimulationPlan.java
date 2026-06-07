package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeLifecycleSimulationPlan(
        String simulationId,
        List<String> lifecyclePhases,
        List<String> moduleLoadOrder,
        boolean executesAddonCode,
        boolean dryRunOnly
) {
}
