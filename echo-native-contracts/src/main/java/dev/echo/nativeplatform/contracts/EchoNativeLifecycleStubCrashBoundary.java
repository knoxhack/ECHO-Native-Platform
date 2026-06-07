package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeLifecycleStubCrashBoundary(
        String boundaryId,
        boolean verified,
        boolean stubFailureContained,
        boolean lifecycleOrderValidated,
        boolean shutdownBoundaryValidated,
        boolean diagnosticsWritten,
        boolean realAddonCodeExecuted,
        boolean classloaderCreated,
        boolean registryInjected,
        List<String> containedFailureModes
) {
}
