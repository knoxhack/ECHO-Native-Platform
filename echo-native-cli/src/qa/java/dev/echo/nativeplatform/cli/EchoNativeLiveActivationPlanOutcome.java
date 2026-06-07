package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeLiveActivationPlanOutcome(
        String packId,
        Map<String, Object> nativeLiveActivationWrapperPlan,
        Map<String, Object> nativeLiveActivationSafetyGate,
        Map<String, Object> nativeLiveActivationMarkerContract,
        Map<String, Object> phase13M22Readiness,
        List<EchoNativeDiagnostic> diagnostics
) {
}
