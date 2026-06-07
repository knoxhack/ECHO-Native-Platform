package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativePhase12GatePlan(
        String packId,
        Map<String, Object> completion,
        Map<String, Object> phase13Readiness,
        List<EchoNativeDiagnostic> diagnostics
) {
}
