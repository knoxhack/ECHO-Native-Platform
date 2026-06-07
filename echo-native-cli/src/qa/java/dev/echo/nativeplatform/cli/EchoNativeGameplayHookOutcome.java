package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeGameplayHookOutcome(
        String packId,
        Map<String, Object> nativeProductGameplayHookEvidence,
        Map<String, Object> nativeModuleGameplayHookStatus,
        Map<String, Object> nativeProductPlayableReadiness,
        Map<String, Object> phase13M23Completion,
        Map<String, Object> phase13M24Readiness,
        List<EchoNativeDiagnostic> diagnostics
) {
}
