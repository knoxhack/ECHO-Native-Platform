package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeClasspathSourcePolicy(
        String policyId,
        boolean reportInputsOnly,
        boolean plannedEntriesOnly,
        boolean classloaderCreationAllowed,
        boolean runtimeClassResolutionAllowed,
        boolean filesystemMutated,
        List<String> trustedSources,
        List<String> blockedSources
) {
}
