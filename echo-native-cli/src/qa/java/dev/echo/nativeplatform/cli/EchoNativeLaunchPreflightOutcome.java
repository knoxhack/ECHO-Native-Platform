package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeLaunchPreflightOutcome(
        String packId,
        Map<String, Object> isolatedLaunchEnvironmentPlan,
        Map<String, Object> minecraftLaunchPreflight,
        Map<String, Object> launchSafetyGate,
        Map<String, Object> controlledLaunchFailureCapturePlan,
        Map<String, Object> phase13M17Readiness,
        List<EchoNativeDiagnostic> diagnostics
) {
}
