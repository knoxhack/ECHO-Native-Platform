package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativePhase13BridgeRehearsalOutcome(
        String packId,
        Map<String, Object> resourceBridgePolicyRehearsal,
        Map<String, Object> registryBridgePolicyRehearsal,
        Map<String, Object> phase13BridgeSafetyStatus,
        List<EchoNativeDiagnostic> diagnostics
) {
}
