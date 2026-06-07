package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeRuntimeFixtureHandoffOutcome(
        String packId,
        Map<String, Object> runtimeFixtureHandoff,
        Map<String, Object> runtimeFixtureValidationRunbook,
        List<EchoNativeDiagnostic> diagnostics
) {
}
