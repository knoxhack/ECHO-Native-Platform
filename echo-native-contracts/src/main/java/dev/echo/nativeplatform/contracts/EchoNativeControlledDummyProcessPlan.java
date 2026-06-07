package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeControlledDummyProcessPlan(
        String planId,
        boolean ready,
        boolean dummyProcessOnly,
        boolean processLaunchAllowed,
        boolean gameProcessLaunchAllowed,
        boolean commandExecutionAllowed,
        boolean minecraftLaunchAllowed,
        boolean classloaderCreationAllowed,
        boolean runtimeClassResolutionAllowed,
        boolean filesystemMutationAllowed,
        long timeoutMillis,
        List<String> sanitizedCommand,
        List<String> requiredInputs
) {
}
