package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeBetaSoakEvidenceAuditOutcome(
        String packId,
        Map<String, Object> nativeLoaderBetaEvidenceQuality,
        Map<String, Object> nativeLoaderBetaSessionProofMatrix,
        Map<String, Object> phase13M29EvidenceGap,
        List<EchoNativeDiagnostic> diagnostics
) {
}
