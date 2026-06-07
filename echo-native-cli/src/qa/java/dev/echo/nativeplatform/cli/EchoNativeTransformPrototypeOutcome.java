package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeTransformPrototypeOutcome(
        String packId,
        Map<String, Object> transformSourceInventory,
        Map<String, Object> transformAllowlistValidation,
        Map<String, Object> transformPipelinePlan,
        Map<String, Object> transformConflictReport,
        Map<String, Object> transformSafetyStatus,
        List<EchoNativeDiagnostic> diagnostics
) {
}
