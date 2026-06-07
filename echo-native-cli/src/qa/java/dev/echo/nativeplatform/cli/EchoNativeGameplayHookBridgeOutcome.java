package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeGameplayHookBridgeOutcome(
        String packId,
        Map<String, Object> gameplayHookBridgePlan,
        Map<String, Object> gameplayHookSignalContract,
        Map<String, Object> gameplayHookSignalStatus,
        Map<String, Object> nativeProductModuleGameplayActivation,
        Map<String, Object> phase13M24Completion,
        Map<String, Object> nativeProductPlayableGate,
        Map<String, Object> phase13M25Readiness,
        List<EchoNativeDiagnostic> diagnostics
) {
}
