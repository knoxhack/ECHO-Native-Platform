package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeNativeExtractionSourcePolicy(
        String policyId,
        boolean reportInputsOnly,
        boolean nativeExtractionAllowed,
        boolean nativeFilesExtracted,
        boolean filesystemMutationAllowed,
        boolean runtimeNativeLookupAllowed,
        List<String> trustedSources,
        List<String> blockedSources
) {
}
