package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeRuntimeFixtureApprovalAuditOutcome(
        String packId,
        Map<String, Object> runtimeFixtureApprovalAudit,
        Map<String, Object> runtimeFixtureApprovalTemplate,
        List<EchoNativeDiagnostic> diagnostics
) {
}
