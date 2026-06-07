package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeRealProcessLaunchHarnessOutcome(
        String packId,
        Map<String, Object> realProcessLaunchHarnessPlan,
        Map<String, Object> realProcessLaunchSafetyGate,
        Map<String, Object> realProcessCommandLinePreview,
        Map<String, Object> realProcessEnvironmentPlan,
        List<EchoNativeDiagnostic> diagnostics
) {
}
