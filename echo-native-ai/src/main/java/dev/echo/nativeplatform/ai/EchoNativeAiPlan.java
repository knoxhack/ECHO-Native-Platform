package dev.echo.nativeplatform.ai;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoNativeAiPlan(
        String packId,
        Map<String, Object> aiGraph,
        Map<String, Object> aiTasks,
        List<EchoNativeDiagnostic> diagnostics
) {
}
