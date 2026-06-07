package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeNetworkPrototypeOutcome(
        String packId,
        Map<String, Object> networkChannelInventory,
        Map<String, Object> networkPacketValidation,
        Map<String, Object> networkSchemaModel,
        Map<String, Object> networkConflictReport,
        Map<String, Object> networkBridgeSafetyStatus,
        List<EchoNativeDiagnostic> diagnostics
) {
}
