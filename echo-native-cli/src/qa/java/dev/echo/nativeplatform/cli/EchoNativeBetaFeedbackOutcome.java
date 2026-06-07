package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeBetaFeedbackOutcome(
        String packId,
        Map<String, Object> nativeLoaderBetaFeedbackInventory,
        Map<String, Object> nativeLoaderBetaCrashIntake,
        Map<String, Object> nativeLoaderBetaKnownIssues,
        Map<String, Object> nativeLoaderBetaNextActionQueue,
        Map<String, Object> phase13M27Completion,
        Map<String, Object> phase13M28Readiness,
        List<EchoNativeDiagnostic> diagnostics
) {
}
