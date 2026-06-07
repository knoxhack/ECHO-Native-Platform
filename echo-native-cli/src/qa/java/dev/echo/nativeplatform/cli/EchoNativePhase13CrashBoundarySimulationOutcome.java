package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativePhase13CrashBoundarySimulationOutcome(
        String packId,
        Map<String, Object> crashBoundarySimulationResult,
        Map<String, Object> boundaryFailureCases,
        Map<String, Object> classloaderBoundaryRehearsal,
        List<EchoNativeDiagnostic> diagnostics
) {
}
