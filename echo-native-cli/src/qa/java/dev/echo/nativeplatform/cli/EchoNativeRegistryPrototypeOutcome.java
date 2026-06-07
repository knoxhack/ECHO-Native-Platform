package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeRegistryPrototypeOutcome(
        String packId,
        Map<String, Object> registrySourceInventory,
        Map<String, Object> registryIdValidation,
        Map<String, Object> sandboxRegistryModel,
        Map<String, Object> registryConflictReport,
        Map<String, Object> registryBridgeSafetyStatus,
        List<EchoNativeDiagnostic> diagnostics
) {
}
