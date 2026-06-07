package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeTesterEvidenceOutcome(
        String packId,
        Map<String, Object> testerPlayableEvidence,
        Map<String, Object> minecraftBaselinePlayability,
        Map<String, Object> nativeProductPlayableGap,
        Map<String, Object> phase13M20Completion,
        Map<String, Object> phase13M21Readiness,
        List<EchoNativeDiagnostic> diagnostics
) {
}
