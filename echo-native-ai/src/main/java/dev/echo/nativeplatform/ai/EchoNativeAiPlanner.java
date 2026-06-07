package dev.echo.nativeplatform.ai;

import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public final class EchoNativeAiPlanner {
    private static final List<String> LANES = List.of(
            "native_cli_agent",
            "packos_agent",
            "validation_agent",
            "release_agent",
            "docs_agent"
    );

    public EchoNativeAiPlan plan(
            String packId,
            List<EchoNativeAddonDescriptor> descriptors,
            List<EchoNativeDiagnostic> diagnostics,
            Map<String, Object> moduleGraph,
            Map<String, Object> featureGraph,
            Map<String, Object> serviceGraph,
            Map<String, Object> lockfileStatus,
            Map<String, Object> repairPlan
    ) {
        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();
        List<Map<String, Object>> tasks = sortedDiagnostics.stream()
                .filter(this::createsTask)
                .map(this::task)
                .toList();

        Map<String, Object> aiTasks = new LinkedHashMap<>();
        aiTasks.put("dryRunOnly", true);
        aiTasks.put("lanes", LANES);
        aiTasks.put("phase", "phase12_native_ai_dry_run");
        aiTasks.put("phase13Blocked", true);
        aiTasks.put("taskCount", tasks.size());
        aiTasks.put("tasks", tasks);

        Map<String, Object> aiGraph = new LinkedHashMap<>();
        aiGraph.put("diagnosticCount", sortedDiagnostics.size());
        aiGraph.put("dryRunOnly", true);
        aiGraph.put("laneSummary", laneSummary(tasks));
        aiGraph.put("lanes", LANES);
        aiGraph.put("moduleCount", descriptors.size());
        aiGraph.put("packId", packId);
        aiGraph.put("phase", "phase12_native_ai_dry_run");
        aiGraph.put("phase13Blocked", true);
        aiGraph.put("phase13Unlock", "Complete the native dry-run validation, graph, feature, lock, repair, AI graph, report, bootstrap, and snapshot gates before prototype loader work.");
        aiGraph.put("sourceReports", sourceReports(moduleGraph, featureGraph, serviceGraph, lockfileStatus, repairPlan));
        aiGraph.put("taskCount", tasks.size());
        return new EchoNativeAiPlan(packId, aiGraph, aiTasks, sortedDiagnostics);
    }

    private boolean createsTask(EchoNativeDiagnostic diagnostic) {
        return diagnostic.severity() == EchoNativeIssueSeverity.ERROR || diagnostic.severity() == EchoNativeIssueSeverity.FATAL;
    }

    private Map<String, Object> task(EchoNativeDiagnostic diagnostic) {
        String lane = lane(diagnostic);
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("acceptanceCriteria", acceptanceCriteria(diagnostic));
        task.put("agentLane", lane);
        task.put("commands", commands(diagnostic));
        task.put("diagnosticCode", diagnostic.code());
        task.put("id", "phase12.native." + slug(diagnostic.code()) + "." + taskTarget(diagnostic));
        task.put("moduleId", diagnostic.moduleId());
        task.put("packId", diagnostic.packId());
        task.put("priority", priority(diagnostic));
        task.put("protectedCapabilities", List.of("minecraft.launch", "bytecode.transforms", "registry.injection", "native.library.extraction"));
        task.put("safeEditZones", safeEditZones(diagnostic));
        task.put("source", "native_diagnostics");
        task.put("status", "ready");
        task.put("summary", diagnostic.summary());
        task.put("title", diagnostic.title());
        return task;
    }

    private String lane(EchoNativeDiagnostic diagnostic) {
        return switch (diagnostic.code()) {
            case "ECHO-NATIVE-LOCKFILE-MISSING", "ECHO-NATIVE-LOCKFILE-DRIFT", "ECHO-NATIVE-LOCKFILE-INVALID" -> "packos_agent";
            case "ECHO-NATIVE-REQUIRED-MODULE-MISSING", "ECHO-NATIVE-ROOT-MODULE-MISSING", "ECHO-NATIVE-REQUIRED-FEATURE-MISSING",
                    "ECHO-NATIVE-MODULE-DUPLICATE", "ECHO-NATIVE-DESCRIPTOR-INVALID", "ECHO-NATIVE-DESCRIPTOR-SCHEMA",
                    "ECHO-NATIVE-DESCRIPTOR-ID-MISSING" -> "validation_agent";
            case "ECHO-NATIVE-TRANSFORMS-BLOCKED" -> "native_cli_agent";
            default -> "native_cli_agent";
        };
    }

    private String priority(EchoNativeDiagnostic diagnostic) {
        return diagnostic.severity() == EchoNativeIssueSeverity.FATAL ? "P0" : "P1";
    }

    private List<String> commands(EchoNativeDiagnostic diagnostic) {
        String fixture = diagnostic.packId() == null || diagnostic.packId().isBlank() ? "<fixture>" : "fixtures/" + diagnostic.packId().replace('_', '-');
        return List.of(
                "echo-native validate " + fixture,
                "echo-native ai graph " + fixture
        );
    }

    private List<String> acceptanceCriteria(EchoNativeDiagnostic diagnostic) {
        return List.of(
                "Diagnostic " + diagnostic.code() + " is resolved or explicitly documented as accepted dry-run risk.",
                "Native reports remain deterministic and secret-safe.",
                "No prototype loader behavior is started."
        );
    }

    private List<String> safeEditZones(EchoNativeDiagnostic diagnostic) {
        TreeSet<String> zones = new TreeSet<>(diagnostic.likelyFiles());
        zones.add("echo-native-platform/fixtures");
        zones.add("echo-native-platform/docs");
        return List.copyOf(zones);
    }

    private Map<String, Object> laneSummary(List<Map<String, Object>> tasks) {
        Map<String, Integer> counts = new TreeMap<>();
        for (String lane : LANES) {
            counts.put(lane, 0);
        }
        for (Map<String, Object> task : tasks) {
            String lane = String.valueOf(task.get("agentLane"));
            counts.put(lane, counts.getOrDefault(lane, 0) + 1);
        }
        return new LinkedHashMap<>(counts);
    }

    private Map<String, Object> sourceReports(
            Map<String, Object> moduleGraph,
            Map<String, Object> featureGraph,
            Map<String, Object> serviceGraph,
            Map<String, Object> lockfileStatus,
            Map<String, Object> repairPlan
    ) {
        Map<String, Object> sources = new LinkedHashMap<>();
        sources.put("featureGraph", Map.of(
                "featureCount", value(featureGraph, "featureCount"),
                "missingOptional", value(featureGraph, "missingOptional"),
                "missingRequired", value(featureGraph, "missingRequired")
        ));
        sources.put("lockfileStatus", Map.of(
                "status", value(lockfileStatus, "status"),
                "verifiedModules", value(lockfileStatus, "verifiedModules")
        ));
        sources.put("moduleGraph", Map.of(
                "edgeCount", value(moduleGraph, "edgeCount"),
                "nodeCount", value(moduleGraph, "nodeCount")
        ));
        sources.put("repairPlan", Map.of(
                "actionCount", value(repairPlan, "actionCount"),
                "status", value(repairPlan, "status")
        ));
        sources.put("serviceGraph", Map.of(
                "serviceCount", value(serviceGraph, "serviceCount")
        ));
        return sources;
    }

    private Object value(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? 0 : value;
    }

    private String taskTarget(EchoNativeDiagnostic diagnostic) {
        if (diagnostic.moduleId() != null && !diagnostic.moduleId().isBlank()) {
            return slug(diagnostic.moduleId());
        }
        return Integer.toHexString(diagnostic.summary().hashCode());
    }

    private String slug(String value) {
        List<Character> result = new ArrayList<>();
        for (char item : value.toLowerCase().toCharArray()) {
            result.add((item >= 'a' && item <= 'z') || (item >= '0' && item <= '9') ? item : '-');
        }
        StringBuilder builder = new StringBuilder(result.size());
        result.forEach(builder::append);
        return builder.toString().replaceAll("-+", "-").replaceAll("^-|-$", "");
    }
}
