package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeResourcePrototypeOutcome(
        String packId,
        Map<String, Object> resourceSourceInventory,
        Map<String, Object> resourceNamespaceValidation,
        Map<String, Object> resourcePackOrderPlan,
        Map<String, Object> resourceConflictReport,
        Map<String, Object> resourceBridgeSafetyStatus,
        List<EchoNativeDiagnostic> diagnostics
) {
}
