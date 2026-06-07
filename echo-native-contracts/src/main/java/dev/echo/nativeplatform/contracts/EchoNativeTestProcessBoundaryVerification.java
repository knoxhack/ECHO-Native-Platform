package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeTestProcessBoundaryVerification(
        String verificationId,
        boolean verified,
        boolean processLaunched,
        boolean gameProcessLaunched,
        boolean classloaderCreated,
        boolean resolvesRuntimeClasses,
        boolean filesystemMutated,
        boolean commandExecuted,
        List<String> verifiedInputs,
        List<String> blockedCapabilities
) {
}
