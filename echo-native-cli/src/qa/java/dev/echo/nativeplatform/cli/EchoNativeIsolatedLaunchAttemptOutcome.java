package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeIsolatedLaunchAttemptOutcome(
        String packId,
        Map<String, Object> isolatedLaunchAttemptPlan,
        Map<String, Object> localRuntimeArtifactCheck,
        Map<String, Object> controlledLaunchAttemptResult,
        Map<String, Object> launchOutputCapture,
        Map<String, Object> phase13M17LaunchStatus,
        List<EchoNativeDiagnostic> diagnostics
) {
}
