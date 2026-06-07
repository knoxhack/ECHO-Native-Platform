package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeBetaSessionDraftOutcome(
        String packId,
        Map<String, Object> nativeLoaderBetaSessionDraftFiles,
        Map<String, Object> phase13M29SessionDraftStatus,
        List<EchoNativeDiagnostic> diagnostics
) {
}
