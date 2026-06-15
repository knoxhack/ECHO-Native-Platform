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
        int totalFeatures = 0;
        int totalExportPlans = 0;
        int totalHytaleBlockers = 0;

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
            Path graphDir = graphPath.getParent();
            int features = featureCount(graphDir);
            int exportPlans = exportPlanCount(graphDir);
            int hytaleBlockers = hytaleBlockerCount(graphDir);
            totalNodes += nodes;
            totalEdges += edges;
            totalFeatures += features;
            totalExportPlans += exportPlans;
            totalHytaleBlockers += hytaleBlockers;

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("moduleId", moduleId);
            summary.put("graphPath", graphPath.toString());
            summary.put("schemaVersion", String.valueOf(graph.getOrDefault("schemaVersion", "unknown")));
            summary.put("nodes", nodes);
            summary.put("edges", edges);
            summary.put("features", features);
            summary.put("exportPlans", exportPlans);
            summary.put("hytaleBlockers", hytaleBlockers);
            loadedModules.add(summary);
        }

        return new EchoNativeContentGraph(
                "echo.content_graph.evidence.v1",
                root.toString(),
                loadedModules.size(),
                totalNodes,
                totalEdges,
                totalFeatures,
                totalExportPlans,
                totalHytaleBlockers,
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

    private static int featureCount(Path graphDir) {
        if (graphDir == null) {
            return 0;
        }
        Path featuresPath = graphDir.resolve("features.json");
        if (!Files.isRegularFile(featuresPath)) {
            return 0;
        }
        try {
            Map<String, Object> features = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(featuresPath)));
            return listSize(features.get("features"));
        } catch (IOException | RuntimeException ignored) {
            return 0;
        }
    }

    private static int exportPlanCount(Path graphDir) {
        if (graphDir == null) {
            return 0;
        }
        Path exportPlanDir = graphDir.resolve("export-plans");
        if (!Files.isDirectory(exportPlanDir)) {
            return 0;
        }
        try (Stream<Path> stream = Files.list(exportPlanDir)) {
            return (int) stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .count();
        } catch (IOException ignored) {
            return 0;
        }
    }

    private static int hytaleBlockerCount(Path graphDir) {
        if (graphDir == null) {
            return 0;
        }
        Path hytalePath = graphDir.resolve("export-plans").resolve("hytale.json");
        if (!Files.isRegularFile(hytalePath)) {
            return 0;
        }
        try {
            Map<String, Object> hytale = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(hytalePath)));
            Object nodes = hytale.get("nodes");
            if (nodes instanceof List<?> list) {
                int blocked = 0;
                for (Object node : list) {
                    if (node instanceof Map<?, ?> map && "blocked".equals(map.get("status"))) {
                        blocked++;
                    }
                }
                return blocked;
            }
            Object summary = hytale.get("summary");
            if (summary instanceof Map<?, ?> map && map.get("blocked") instanceof Number number) {
                return number.intValue();
            }
        } catch (IOException | RuntimeException ignored) {
            return 0;
        }
        return 0;
    }

    private static EchoNativeContentGraph emptyGraph(List<EchoNativeDiagnostic> diagnostics) {
        return new EchoNativeContentGraph(
                "echo.content_graph.evidence.v1",
                "",
                0,
                0,
                0,
                0,
                0,
                0,
                List.of(),
                List.copyOf(diagnostics)
        );
    }
}
