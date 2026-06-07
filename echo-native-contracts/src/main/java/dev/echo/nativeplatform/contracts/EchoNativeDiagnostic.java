package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.BETA, since = "0.1.0-native-beta")
public record EchoNativeDiagnostic(
        String code,
        EchoNativeIssueSeverity severity,
        String title,
        String summary,
        String moduleId,
        String packId,
        List<String> likelyFiles,
        String suggestedFix
) {
}
