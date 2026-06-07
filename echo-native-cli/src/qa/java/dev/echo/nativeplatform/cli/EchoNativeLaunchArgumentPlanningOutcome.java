package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeLaunchArgumentPlanningOutcome(
        String packId,
        Map<String, Object> launchArgumentPlan,
        Map<String, Object> launchArgumentSourcePolicy,
        Map<String, Object> launchArgumentSafetyStatus,
        List<EchoNativeDiagnostic> diagnostics
) {
}
