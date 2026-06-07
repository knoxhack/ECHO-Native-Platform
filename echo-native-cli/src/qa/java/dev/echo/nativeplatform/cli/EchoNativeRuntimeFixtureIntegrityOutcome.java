package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeRuntimeFixtureIntegrityOutcome(
        String packId,
        Map<String, Object> runtimeFixtureIntegrityAudit,
        Map<String, Object> runtimeFixtureIntegrityManifest,
        List<EchoNativeDiagnostic> diagnostics
) {
}
