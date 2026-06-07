package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeDummyProcessOutcome(
        String packId,
        Map<String, Object> controlledDummyProcessPlan,
        Map<String, Object> controlledDummyProcessResult,
        Map<String, Object> dummyProcessCrashBoundary,
        Map<String, Object> dummyProcessOutputCapture,
        List<EchoNativeDiagnostic> diagnostics
) {
}
