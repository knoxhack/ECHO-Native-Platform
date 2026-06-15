package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeContentGraph;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;

import java.util.List;
import java.util.Map;

public record EchoNativeGraphPlan(
        List<EchoNativeDiagnostic> diagnostics,
        List<String> moduleLoadOrder,
        Map<String, Object> moduleGraph,
        Map<String, Object> featureGraph,
        Map<String, Object> serviceGraph,
        Map<String, Object> lifecyclePlan,
        EchoNativeContentGraph contentGraph
) {
}
