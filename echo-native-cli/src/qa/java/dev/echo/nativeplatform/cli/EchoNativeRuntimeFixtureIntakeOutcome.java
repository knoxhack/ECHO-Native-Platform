package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeRuntimeFixtureIntakeOutcome(
        String packId,
        Map<String, Object> runtimeFixtureIntakePlan,
        Map<String, Object> runtimeFixtureIntakeChecklist,
        List<EchoNativeDiagnostic> diagnostics
) {
}
