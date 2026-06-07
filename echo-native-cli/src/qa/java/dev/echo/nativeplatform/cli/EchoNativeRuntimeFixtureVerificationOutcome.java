package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeRuntimeFixtureVerificationOutcome(
        String packId,
        Map<String, Object> runtimeFixturePresence,
        Map<String, Object> runtimeFixtureMappingReadiness,
        List<EchoNativeDiagnostic> diagnostics
) {
}
