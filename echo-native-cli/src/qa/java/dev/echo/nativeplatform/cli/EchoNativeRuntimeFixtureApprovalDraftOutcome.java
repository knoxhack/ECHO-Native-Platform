package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeRuntimeFixtureApprovalDraftOutcome(
        String packId,
        Map<String, Object> runtimeFixtureApprovalDraft,
        Map<String, Object> runtimeFixtureHashReview,
        List<EchoNativeDiagnostic> diagnostics
) {
}
