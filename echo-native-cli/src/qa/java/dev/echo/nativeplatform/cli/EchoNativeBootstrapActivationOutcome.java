package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeBootstrapActivationOutcome(
        String packId,
        Map<String, Object> nativeBootstrapActivationResult,
        Map<String, Object> nativeLiveActivationMarker,
        Map<String, Object> nativeLiveActivationSafetyStatus,
        Map<String, Object> phase13M22Completion,
        Map<String, Object> phase13M23Readiness,
        List<EchoNativeDiagnostic> diagnostics
) {
}
