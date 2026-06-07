package dev.echo.nativeplatform.packos;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoNativeLockfileVerificationPlan(
        String packId,
        Map<String, Object> status,
        List<EchoNativeDiagnostic> diagnostics
) {
}
