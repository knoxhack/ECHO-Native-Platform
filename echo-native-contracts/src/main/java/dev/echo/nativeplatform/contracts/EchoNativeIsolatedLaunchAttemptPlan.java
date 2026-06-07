package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeIsolatedLaunchAttemptPlan(
        String planId,
        boolean isolated,
        boolean launchAttemptAllowed,
        boolean launchAttempted,
        boolean controlledFailure,
        boolean timeoutPlanned,
        boolean outputCapturePlanned,
        boolean userInstallMutationAllowed,
        boolean packMutationAllowed,
        boolean saveMutationAllowed,
        boolean configMutationAllowed,
        String workingDirectory,
        List<String> requiredLocalArtifacts
) {
}
