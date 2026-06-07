package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeMinecraftVersionSourcePolicy(
        String policyId,
        boolean localSourcesOnly,
        boolean networkAllowed,
        boolean remoteManifestDownloaded,
        boolean cacheMutationAllowed,
        boolean filesystemMutated,
        List<String> allowedSources,
        List<String> blockedSources
) {
}
