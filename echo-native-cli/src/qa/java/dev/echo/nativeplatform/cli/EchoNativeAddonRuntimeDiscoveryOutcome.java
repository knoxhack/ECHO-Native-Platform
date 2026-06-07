package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeAddonRuntimeDiscoveryOutcome(
        String packId,
        Map<String, Object> addonRuntimeDiscoveryPlan,
        Map<String, Object> addonRuntimeDescriptors,
        Map<String, Object> addonRuntimeDiscoverySafetyStatus,
        List<EchoNativeDiagnostic> diagnostics
) {
}
