package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeBetaSoakOperatorPacketOutcome(
        String packId,
        Map<String, Object> nativeLoaderBetaSoakOperatorPacket,
        Map<String, Object> nativeLoaderBetaSessionTemplate,
        Map<String, Object> nativeLoaderBetaSessionNoteDrafts,
        Map<String, Object> nativeLoaderBetaEvidenceChecklist,
        Map<String, Object> nativeLoaderBetaRemainingSessionPlan,
        Map<String, Object> phase13M29SoakOperatorStatus,
        List<EchoNativeDiagnostic> diagnostics
) {
}
