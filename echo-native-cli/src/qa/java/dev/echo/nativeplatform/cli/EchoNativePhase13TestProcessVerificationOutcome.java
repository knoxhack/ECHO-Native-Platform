package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativePhase13TestProcessVerificationOutcome(
        String packId,
        Map<String, Object> testProcessBoundaryVerification,
        Map<String, Object> controlledTestProcessPreflight,
        Map<String, Object> phase13M1SafetyStatus,
        List<EchoNativeDiagnostic> diagnostics
) {
}
