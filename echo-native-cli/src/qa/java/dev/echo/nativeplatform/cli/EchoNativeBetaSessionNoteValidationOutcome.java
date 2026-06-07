package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeBetaSessionNoteValidationOutcome(
        String packId,
        Map<String, Object> nativeLoaderBetaSessionNoteValidation,
        Map<String, Object> phase13M29NoteValidationStatus,
        List<EchoNativeDiagnostic> diagnostics
) {
}
