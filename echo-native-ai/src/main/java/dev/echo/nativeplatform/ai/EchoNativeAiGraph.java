package dev.echo.nativeplatform.ai;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAiGraph {
    public Map<String, Object> fromDiagnostics(List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> graph = new LinkedHashMap<>();
        graph.put("taskCount", diagnostics.stream().filter(diagnostic -> diagnostic.severity().ordinal() >= EchoNativeIssueSeverity.ERROR.ordinal()).count());
        graph.put("lanes", List.of("native_cli_agent", "packos_agent", "validation_agent", "release_agent", "docs_agent"));
        graph.put("phase13Blocked", true);
        graph.put("phase13Unlock", "Complete native dry-run scan, validate, graph, lock, repair, AI graph, report, and bootstrap gates before prototype loader work.");
        return graph;
    }
}
