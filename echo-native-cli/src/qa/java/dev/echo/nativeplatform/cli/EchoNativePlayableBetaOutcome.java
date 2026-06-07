package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

record EchoNativePlayableBetaOutcome(
        String packId,
        Map<String, Object> phase13M26Completion,
        Map<String, Object> nativeLoaderPlayableBetaReadiness,
        Map<String, Object> nativeProductLoaderBetaStatus,
        Map<String, Object> internalTesterBetaGate,
        List<EchoNativeDiagnostic> diagnostics
) {
}
