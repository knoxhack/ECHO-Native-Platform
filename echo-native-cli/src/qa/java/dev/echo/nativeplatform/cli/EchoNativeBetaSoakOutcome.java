package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeBetaSoakOutcome(
        String packId,
        Map<String, Object> nativeLoaderBetaSoakPlan,
        Map<String, Object> nativeLoaderBetaSessionInventory,
        Map<String, Object> nativeLoaderBetaIssueTriage,
        Map<String, Object> nativeLoaderBetaRegressionWatchlist,
        Map<String, Object> phase13M29Completion,
        Map<String, Object> phase13M30Readiness,
        List<EchoNativeDiagnostic> diagnostics
) {
}
