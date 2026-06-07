package dev.echo.nativeplatform.contracts;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeControlledLaunchAttemptResult(
        String resultId,
        boolean launchAttempted,
        boolean controlledFailure,
        boolean processLaunched,
        boolean gameProcessLaunched,
        boolean mainMenuReached,
        boolean timeoutTriggered,
        int exitCode,
        String failureReason
) {
}
