package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeDummyProcessOutputCapture(
        String captureId,
        boolean captured,
        boolean deterministic,
        boolean secretSafe,
        int stdoutLineCount,
        int stderrLineCount,
        List<String> stdoutLines,
        List<String> stderrLines
) {
}
