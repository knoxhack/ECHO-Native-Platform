package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeLibraryResolverPlanningOutcome(
        String packId,
        Map<String, Object> libraryResolutionPlan,
        Map<String, Object> librarySourcePolicy,
        Map<String, Object> libraryResolverSafetyStatus,
        List<EchoNativeDiagnostic> diagnostics
) {
}
