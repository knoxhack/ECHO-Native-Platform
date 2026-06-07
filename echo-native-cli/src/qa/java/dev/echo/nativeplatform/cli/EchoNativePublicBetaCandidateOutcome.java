package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativePublicBetaCandidateOutcome(
        String packId,
        Map<String, Object> nativeLoaderPublicBetaCandidateAudit,
        Map<String, Object> nativeLoaderPublicBetaSafetyGate,
        Map<String, Object> publicBetaTesterReadiness,
        Map<String, Object> phase13M30Completion,
        Map<String, Object> phase13M31Readiness,
        List<EchoNativeDiagnostic> diagnostics
) {
}
