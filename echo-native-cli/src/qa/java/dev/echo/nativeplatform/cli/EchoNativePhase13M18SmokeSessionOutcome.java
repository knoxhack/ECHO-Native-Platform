package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativePhase13M18SmokeSessionOutcome(
        String packId,
        Map<String, Object> smokeSessionPlan,
        Map<String, Object> smokeSessionSafetyGate,
        Map<String, Object> smokeSessionResult,
        Map<String, Object> smokeSessionDiagnostics,
        Map<String, Object> phase13M18Completion,
        Map<String, Object> phase13M19Readiness,
        List<EchoNativeDiagnostic> diagnostics
) {
}
