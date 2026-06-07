package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativeRuntimeFixtureOperatorPacketOutcome(
        String packId,
        Map<String, Object> runtimeFixtureOperatorPacket,
        List<EchoNativeDiagnostic> diagnostics
) {
}
