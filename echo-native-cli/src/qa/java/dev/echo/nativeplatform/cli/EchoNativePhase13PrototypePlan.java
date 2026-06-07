package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativePhase13PrototypePlan(
        String packId,
        Map<String, Object> phase13Plan,
        Map<String, Object> lifecycleSimulationPlan,
        Map<String, Object> classloaderBoundaryPlan,
        Map<String, Object> crashBoundaryPlan,
        Map<String, Object> testProcessPlan,
        List<EchoNativeDiagnostic> diagnostics
) {
}
