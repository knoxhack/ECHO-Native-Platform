package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativePhase14PreflightOutcome(
        String packId,
        Map<String, Object> firstPlaytestPostOpenIntake,
        Map<String, Object> firstPlaytestFeedbackInventory,
        Map<String, Object> firstPlaytestWaitingChecklist,
        Map<String, Object> phase14PreflightAudit,
        Map<String, Object> phase14Readiness,
        Map<String, Object> phase14NextActions,
        List<EchoNativeDiagnostic> diagnostics
) {
}
