package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeLocalRuntimeArtifactCheck(
        String checkId,
        boolean localArtifactsReady,
        boolean missingArtifactsBecomeDiagnostics,
        boolean downloadAllowed,
        boolean libraryDownloadStarted,
        boolean nativeExtractionStarted,
        int checkedArtifactCount,
        int missingArtifactCount,
        List<Map<String, Object>> artifacts
) {
}
