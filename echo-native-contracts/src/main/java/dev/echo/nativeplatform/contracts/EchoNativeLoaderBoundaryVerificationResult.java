package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeLoaderBoundaryVerificationResult(
        String verificationId,
        boolean verified,
        boolean classloaderCreated,
        boolean resolvesRuntimeClasses,
        boolean processLaunched,
        boolean mutatedFilesystem,
        List<String> verifiedStates
) {
}
