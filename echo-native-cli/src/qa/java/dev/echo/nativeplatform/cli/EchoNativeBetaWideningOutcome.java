package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeBetaWideningOutcome(
        String packId,
        Map<String, Object> nativeLoaderBetaWideningPlan,
        Map<String, Object> nativeLoaderBetaWideningSafetyGate,
        Map<String, Object> betaTesterCohortPlan,
        Map<String, Object> phase13M28Completion,
        Map<String, Object> phase13M29Readiness,
        List<EchoNativeDiagnostic> diagnostics
) {
}
