package dev.echo.nativeplatform.packos;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoNativeLockfilePlan(
        String packId,
        Map<String, Object> lockfile,
        List<EchoNativeDiagnostic> diagnostics
) {
}
