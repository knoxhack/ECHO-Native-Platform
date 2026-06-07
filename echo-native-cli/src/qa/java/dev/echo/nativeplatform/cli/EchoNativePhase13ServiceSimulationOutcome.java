package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativePhase13ServiceSimulationOutcome(
        String packId,
        Map<String, Object> serviceAttachSimulationResult,
        Map<String, Object> crashBoundaryVerification,
        List<EchoNativeDiagnostic> diagnostics
) {
}
