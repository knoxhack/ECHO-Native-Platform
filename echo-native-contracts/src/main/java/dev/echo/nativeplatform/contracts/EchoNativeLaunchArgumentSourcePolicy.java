package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeLaunchArgumentSourcePolicy(
        String policyId,
        boolean reportInputsOnly,
        boolean launchArgumentsPlannedOnly,
        boolean commandExecutionAllowed,
        boolean processLaunchAllowed,
        boolean filesystemMutationAllowed,
        List<String> trustedSources,
        List<String> blockedSources
) {
}
