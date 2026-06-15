package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeContentGraph;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Loads canonical .ECHO Content Graph evidence for native pack planning.
 *
 * <p>This is optional evidence: if no graph root is provided or graphs are missing,
 * the planner returns an empty summary and a warning diagnostic. The native loader
 * does not require a content graph to function.
 */
public final class EchoNativeContentGraphPlanner {

    public EchoNativeContentGraph plan(Path root, List<String> moduleIds) {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        if (root == null || !Files.isDirectory(root)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-CONTENT-GRAPH-ROOT-MISSING",
                    EchoNativeIssueSeverity.WARNING,
                    "Content graph root not available",
                    "No .ECHO Content Graph root was provided; content graph evidence will be empty.",
                    null,
                    null,
                    List.of(),
                    "Set echo.content.graph.root to the directory containing per-module release outputs."
            ));
            return emptyGraph(diagnostics);
        }

        List<String> sortedIds = new ArrayList<>(moduleIds);
        Collections.sort(sortedIds);
        List<Map<String, Object>> loadedModules = new ArrayList<>();
        int totalNodes = 0;
        int totalEdges = 0;

        for (String moduleId : sortedIds) {
            Path graphPath = findContentGraphPath(root, moduleId);
            if (graphPath == null) {
                diagnostics.add(new EchoNativeDiagnostic(
                        "ECHO-NATIVE-CONTENT-GRAPH-MISSING",
                        EchoNativeIssueSeverity.WARNING,
                        "Module content graph missing",
                        "No .echo/content-graph/content-graph.json found for module " + moduleId + ".",
                        moduleId,
                        null,
                        List.of(root.resolve(moduleId).toString()),
                        "Generate content graphs in ECHO-Modules before native planning."
                ));
                continue;
            }

            Map<String, Object> graph;
            try {
                graph = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(graphPath)));
            } catch (IOException | RuntimeException e) {
                diagnostics.add(new EchoNativeDiagnostic(
                        "ECHO-NATIVE-CONTENT-GRAPH-READ-FAILED",
                        EchoNativeIssueSeverity.WARNING,
                        "Failed to read module content graph",
                        "Could not parse " + graphPath + ": " + e.getMessage(),
                        moduleId,
                        null,
                        List.of(graphPath.toString()),
                        "Check that the content graph file is valid JSON."
                ));
                continue;
            }

            int nodes = listSize(graph.get("nodes"));
            int edges = listSize(graph.get("edges"));
            totalNodes += nodes;
            totalEdges += edges;

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("moduleId", moduleId);
            summary.put("graphPath", graphPath.toString());
            summary.put("schemaVersion", String.valueOf(graph.getOrDefault("schemaVersion", "unknown")));
            summary.put("nodes", nodes);
            summary.put("edges", edges);
            loadedModules.add(summary);
        }

        return new EchoNativeContentGraph(
                "echo.native.content_graph.v1",
                root.toString(),
                loadedModules.size(),
                totalNodes,
                totalEdges,
                List.copyOf(loadedModules),
                List.copyOf(diagnostics)
        );
    }

    private static Path findContentGraphPath(Path root, String moduleId) {
        Path moduleDir = root.resolve(moduleId);
        if (!Files.isDirectory(moduleDir)) {
            return null;
        }
        try (Stream<Path> versions = Files.list(moduleDir)) {
            return versions
                    .filter(Files::isDirectory)
                    .map(versionDir -> versionDir.resolve(".echo").resolve("content-graph").resolve("content-graph.json"))
                    .filter(Files::isRegularFile)
                    .findFirst()
                    .orElse(null);
        } catch (IOException ignored) {
            return null;
        }
    }

    private static int listSize(Object value) {
        if (value instanceof List<?> list) {
            return list.size();
        }
        return 0;
    }

    private static EchoNativeContentGraph emptyGraph(List<EchoNativeDiagnostic> diagnostics) {
        return new EchoNativeContentGraph(
                "echo.native.content_graph.v1",
                "",
                0,
                0,
                0,
                List.of(),
                List.copyOf(diagnostics)
        );
    }
}
