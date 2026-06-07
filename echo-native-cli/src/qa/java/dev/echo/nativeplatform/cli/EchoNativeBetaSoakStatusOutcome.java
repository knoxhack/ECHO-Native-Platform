package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeBetaSoakStatusOutcome(
        String packId,
        Map<String, Object> nativeLoaderBetaSoakStatusDashboard,
        Map<String, Object> nativeLoaderBetaNextSessionChecklist,
        List<EchoNativeDiagnostic> diagnostics
) {
}
