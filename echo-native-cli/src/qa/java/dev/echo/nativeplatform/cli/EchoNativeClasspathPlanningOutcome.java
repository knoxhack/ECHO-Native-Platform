package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeClasspathPlanningOutcome(
        String packId,
        Map<String, Object> classpathBuilderPlan,
        Map<String, Object> classpathSourcePolicy,
        Map<String, Object> classpathBuilderSafetyStatus,
        List<EchoNativeDiagnostic> diagnostics
) {
}
