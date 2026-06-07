package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeAddonRuntimeDiscoveryPlan(
        String planId,
        boolean ready,
        boolean dataOnly,
        boolean deterministicOrder,
        boolean addonCodeExecuted,
        boolean classloaderCreated,
        boolean gameClassesResolved,
        boolean filesystemMutated,
        List<String> requiredInputs,
        List<String> discoveryRoots
) {
}
