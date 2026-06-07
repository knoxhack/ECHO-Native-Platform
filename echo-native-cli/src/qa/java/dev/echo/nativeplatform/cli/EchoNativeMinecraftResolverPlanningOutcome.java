package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeMinecraftResolverPlanningOutcome(
        String packId,
        Map<String, Object> minecraftVersionResolverPlan,
        Map<String, Object> minecraftVersionSourcePolicy,
        Map<String, Object> minecraftResolverSafetyStatus,
        List<EchoNativeDiagnostic> diagnostics
) {
}
