package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeServiceBusPlan(
        String planId,
        boolean ready,
        boolean inMemoryOnly,
        boolean inertHandlesOnly,
        boolean serviceCodeExecuted,
        boolean classloaderCreated,
        boolean gameClassesResolved,
        boolean processLaunched,
        boolean registryMutated,
        int plannedServiceCount,
        List<Map<String, Object>> plannedServices
) {
}
