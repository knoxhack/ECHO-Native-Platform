package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeGameplayHookEvidenceOutcome(
        String packId,
        Map<String, Object> ashfallGameplayHookEvidence,
        Map<String, Object> nativeModuleGameplayHookStatus,
        Map<String, Object> ashfallPlayableBetaReadiness,
        Map<String, Object> phase13M23Completion,
        Map<String, Object> phase13M24Readiness,
        List<EchoNativeDiagnostic> diagnostics
) {
}
