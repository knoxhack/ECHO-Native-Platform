package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeArtifactBlockerOutcome(
        String packId,
        Map<String, Object> phase13M17ArtifactBlockers,
        Map<String, Object> phase13M17BlockerResolutionPlan,
        Map<String, Object> phase13M18Readiness,
        List<EchoNativeDiagnostic> diagnostics
) {
}
