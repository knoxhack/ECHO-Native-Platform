package dev.echo.nativeplatform.contracts;

import java.util.List;
import java.util.Map;

/**
 * Optional evidence summarizing a canonical .ECHO Content Graph for a native pack.
 *
 * <p>The native runtime does not depend on this graph for loading; it is emitted by
 * {@code EchoNativeGraphPlanner} as planning evidence and may be consumed by reports,
 diagnostics, and launcher compatibility checks.
 */
public record EchoNativeContentGraph(
        String schemaVersion,
        String source,
        int moduleCount,
        int nodeCount,
        int edgeCount,
        List<Map<String, Object>> modules,
        List<EchoNativeDiagnostic> diagnostics
) {
}
