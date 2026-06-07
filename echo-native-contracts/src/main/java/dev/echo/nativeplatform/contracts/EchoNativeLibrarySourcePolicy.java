package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeLibrarySourcePolicy(
        String policyId,
        boolean localManifestOnly,
        boolean downloadsAllowed,
        boolean remoteManifestAllowed,
        boolean cacheMutationAllowed,
        boolean filesystemMutated,
        List<String> trustedSources,
        List<String> blockedSources
) {
}
