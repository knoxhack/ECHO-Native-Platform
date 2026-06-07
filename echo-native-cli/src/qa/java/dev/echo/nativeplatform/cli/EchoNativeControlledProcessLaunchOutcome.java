package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeControlledProcessLaunchOutcome(
        String packId,
        Map<String, Object> controlledProcessLaunchPlan,
        Map<String, Object> controlledProcessLaunchSafetyGate,
        Map<String, Object> controlledProcessLaunchResult,
        Map<String, Object> controlledProcessOutputCapture,
        Map<String, Object> controlledProcessRollbackStatus,
        List<EchoNativeDiagnostic> diagnostics
) {
}
