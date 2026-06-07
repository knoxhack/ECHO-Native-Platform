package dev.echo.nativeplatform.contracts;

import java.util.List;

@EchoNativeApiStatus(value = EchoNativeApiStability.INTERNAL, since = "0.1.0-native-beta")
public record EchoNativeLaunchArtifactResolutionStatus(
        String statusId,
        boolean artifactsResolved,
        boolean missingArtifactsBecomeDiagnostics,
        boolean downloadsAllowed,
        boolean libraryDownloadStarted,
        boolean nativeExtractionStarted,
        boolean filesystemMutated,
        int resolvedArtifactCount,
        int missingArtifactCount,
        List<String> completedChecks
) {
}
