package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeModuleRuntimeBridgeOutcome(
        String packId,
        Map<String, Object> nativeModuleRuntimeBridgePlan,
        Map<String, Object> nativeModuleRuntimeBridgeSafetyGate,
        Map<String, Object> nativeModuleBootstrapStatus,
        Map<String, Object> nativeProductLiveModuleActivationStatus,
        Map<String, Object> nativeProductPlayableGate,
        List<EchoNativeDiagnostic> diagnostics
) {
}
