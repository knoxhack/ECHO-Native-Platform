package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeLaunchRealityAuditOutcome(
        String packId,
        Map<String, Object> nativeLoaderRealityAudit,
        Map<String, Object> nativeLoaderLaunchCommandClassification,
        Map<String, Object> nativeLoaderBetaImplementationNextActions,
        List<EchoNativeDiagnostic> diagnostics
) {
}
