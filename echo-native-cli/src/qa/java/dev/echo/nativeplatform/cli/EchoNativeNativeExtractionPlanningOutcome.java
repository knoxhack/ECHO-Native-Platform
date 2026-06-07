package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeNativeExtractionPlanningOutcome(
        String packId,
        Map<String, Object> nativeExtractionPlan,
        Map<String, Object> nativeExtractionSourcePolicy,
        Map<String, Object> nativeExtractionSafetyStatus,
        List<EchoNativeDiagnostic> diagnostics
) {
}
