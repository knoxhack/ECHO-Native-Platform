package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeGameplayHookInstrumentationOutcome(
        String packId,
        Map<String, Object> gameplayHookInstrumentationPlan,
        Map<String, Object> gameplayHookSignalWriteResult,
        Map<String, Object> gameplayHookSignalAudit,
        Map<String, Object> phase13M25Completion,
        Map<String, Object> phase13M26Readiness,
        List<EchoNativeDiagnostic> diagnostics
) {
}
