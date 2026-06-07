package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeIsolatedLaunchEnvironmentPlan(
        String planId,
        boolean launchPreflightComplete,
        boolean isolatedDirectoryPlanned,
        boolean userInstallMutationAllowed,
        boolean packMutationAllowed,
        boolean saveMutationAllowed,
        boolean configMutationAllowed,
        boolean filesystemMutated,
        String plannedWorkingDirectory,
        List<String> isolationRules
) {
}
