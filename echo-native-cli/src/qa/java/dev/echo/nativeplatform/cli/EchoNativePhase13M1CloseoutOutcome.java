package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativePhase13M1CloseoutOutcome(
        String packId,
        Map<String, Object> phase13M1Completion,
        Map<String, Object> phase13M2Readiness,
        Map<String, Object> phase13PrototypeSafetyGate,
        List<EchoNativeDiagnostic> diagnostics
) {
}
