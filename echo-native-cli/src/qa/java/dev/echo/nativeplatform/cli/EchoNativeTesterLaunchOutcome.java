package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeTesterLaunchOutcome(
        String packId,
        Map<String, Object> testerLaunchPlan,
        Map<String, Object> testerLaunchSafetyGate,
        Map<String, Object> testerLaunchProcess,
        Map<String, Object> testerLaunchSupportPaths,
        Map<String, Object> productBootstrapActivationPlan,
        Map<String, Object> productModuleActivationStatus,
        List<EchoNativeDiagnostic> diagnostics
) {
}
