package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativePublicBetaOpeningOutcome(
        String packId,
        Map<String, Object> publicBetaOpeningAudit,
        Map<String, Object> publicBetaSafetyGate,
        Map<String, Object> publicBetaTesterPackageReadiness,
        Map<String, Object> publicBetaModuleCoverage,
        Map<String, Object> publicBetaRollbackReadiness,
        Map<String, Object> publicBetaKnownLimitations,
        Map<String, Object> phase13M31Completion,
        Map<String, Object> phase13M32Readiness,
        List<EchoNativeDiagnostic> diagnostics
) {
}
