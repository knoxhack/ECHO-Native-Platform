package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeCrashHardeningOutcome(
        String packId,
        Map<String, Object> crashHardeningCoverage,
        Map<String, Object> failureContainmentMatrix,
        Map<String, Object> supportBundleDryRunPlan,
        Map<String, Object> phase13M16SafetyStatus,
        List<EchoNativeDiagnostic> diagnostics
) {
}
