package dev.echo.nativeplatform.packos;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoNativeRepairPlan(
        String packId,
        Map<String, Object> repairPlan,
        List<EchoNativeDiagnostic> diagnostics
) {
}
