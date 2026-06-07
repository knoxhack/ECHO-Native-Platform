package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativePhase13M17CloseoutOutcome(
        String packId,
        Map<String, Object> phase13M17Completion,
        Map<String, Object> phase13M18ReadinessAudit,
        Map<String, Object> phase13FirstPlaytestBlockers,
        List<EchoNativeDiagnostic> diagnostics
) {
}
