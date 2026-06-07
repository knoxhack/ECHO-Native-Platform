package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeServiceAttachSimulationResult(
        String simulationId,
        boolean simulated,
        boolean executedServiceCode,
        boolean classloaderCreated,
        boolean processLaunched,
        List<String> attachedServices,
        List<String> attachedModules,
        List<String> blockedServices
) {
}
