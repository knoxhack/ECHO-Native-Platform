package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeLaunchSafetyGate(
        String gateId,
        boolean safeForIsolatedLaunchAttempt,
        boolean m16SafetyPassed,
        boolean isolatedEnvironmentPassed,
        boolean failureCapturePlanned,
        boolean commandExecuted,
        boolean processLaunched,
        boolean gameProcessLaunched,
        boolean registryInjected,
        boolean transformsEnabled,
        boolean filesystemMutated,
        List<String> completedChecks
) {
}
