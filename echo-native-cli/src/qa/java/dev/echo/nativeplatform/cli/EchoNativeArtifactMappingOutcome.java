package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeArtifactMappingOutcome(
        String packId,
        Map<String, Object> localRuntimeArtifactMap,
        Map<String, Object> launchArtifactResolutionStatus,
        Map<String, Object> isolatedLaunchExecutionEligibility,
        Map<String, Object> phase13M17ArtifactReadiness,
        List<EchoNativeDiagnostic> diagnostics
) {
}
