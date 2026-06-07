package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeConfigPrototypeOutcome(
        String packId,
        Map<String, Object> configSourceInventory,
        Map<String, Object> configValidationResult,
        Map<String, Object> configWritePlan,
        Map<String, Object> configSafetyStatus,
        List<EchoNativeDiagnostic> diagnostics
) {
}
