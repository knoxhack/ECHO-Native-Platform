package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeLifecycleStubExecutionOutcome(
        String packId,
        Map<String, Object> lifecycleStubExecutionPlan,
        Map<String, Object> lifecycleStubExecutionResult,
        Map<String, Object> lifecycleStubCrashBoundary,
        Map<String, Object> lifecycleStubSafetyStatus,
        List<EchoNativeDiagnostic> diagnostics
) {
}
