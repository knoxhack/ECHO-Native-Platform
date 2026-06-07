package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeExecutionReadinessOutcome(
        String packId,
        Map<String, Object> processExecutionReadiness,
        Map<String, Object> controlledLaunchOperatorChecklist,
        Map<String, Object> controlledLaunchRollbackPlan,
        Map<String, Object> phase13NativeLoaderBetaGate,
        List<EchoNativeDiagnostic> diagnostics
) {
}
