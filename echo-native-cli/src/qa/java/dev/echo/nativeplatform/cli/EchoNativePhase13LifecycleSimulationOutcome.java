package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativePhase13LifecycleSimulationOutcome(
        String packId,
        Map<String, Object> lifecycleSimulationResult,
        Map<String, Object> crashBoundaryResult,
        List<EchoNativeDiagnostic> diagnostics
) {
}
