package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeArtifactPackagingAuditOutcome(
        String packId,
        Map<String, Object> phase13M17ArtifactPackagingAudit,
        Map<String, Object> phase13M17ArtifactPackagingResolutionPlan,
        List<EchoNativeDiagnostic> diagnostics
) {
}
