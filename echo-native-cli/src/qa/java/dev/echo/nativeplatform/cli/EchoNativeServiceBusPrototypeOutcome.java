package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeServiceBusPrototypeOutcome(
        String packId,
        Map<String, Object> serviceBusPlan,
        Map<String, Object> serviceBusRegistry,
        Map<String, Object> serviceBusSimulationResult,
        Map<String, Object> serviceBusSafetyStatus,
        List<EchoNativeDiagnostic> diagnostics
) {
}
