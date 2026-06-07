package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeIsolatedLaunchExecutionEligibility(
        String eligibilityId,
        boolean eligibleForLaunchAttempt,
        boolean upstreamSafetyPassed,
        boolean localArtifactsReady,
        boolean processLaunchStillGated,
        boolean commandExecuted,
        boolean classloaderCreated,
        boolean gameClassesResolved,
        boolean filesystemMutated,
        List<String> requiredReports
) {
}
