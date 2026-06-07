package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeCrashBoundaryVerification(
        String verificationId,
        boolean verified,
        boolean crashBoundaryActive,
        boolean capturedDiagnostics,
        boolean terminatedProcess,
        boolean mutatedState,
        long simulatedFailureCount,
        List<String> verifiedBoundaries
) {
}
