package dev.echo.nativeplatform.contracts;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeLaunchOutputCapture(
        String captureId,
        boolean outputCaptureReady,
        boolean commandExecuted,
        boolean processLaunched,
        boolean stdoutCaptured,
        boolean stderrCaptured,
        boolean secretSafe,
        String stdoutTail,
        String stderrTail
) {
}
