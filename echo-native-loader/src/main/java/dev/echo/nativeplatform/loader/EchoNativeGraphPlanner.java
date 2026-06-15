package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeContentGraph;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativeRuntimeSide;
import dev.echo.nativeplatform.contracts.EchoNativeServiceDescriptor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class EchoNativeGraphPlanner {
    private final EchoNativeValidator validator = new EchoNativeValidator();
    private final EchoNativeContentGraphPlanner contentGraphPlanner = new EchoNativeContentGraphPlanner();

    public EchoNativeGraphPlan plan(EchoNativeScanResult scanResult) {
        return plan(scanResult, contentGraphRoot());
    }

    public EchoNativeGraphPlan plan(EchoNativeScanResult scanResult, Path contentGraphRoot) {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>(validator.validate(scanResult));
        EchoNativeContentGraph contentGraph = contentGraphRoot == null
                ? contentGraphPlanner.plan(null, List.of())
                : contentGraphPlanner.plan(contentGraphRoot, moduleIds(scanResult));
        if (scanResult.packProfile() == null) {
            return new EchoNativeGraphPlan(
                    diagnostics,
                    List.of(),
                    Map.of("nodes", List.of(), "edges", List.of()),
                    Map.of("features", List.of(), "edges", List.of()),
                    Map.of("services", List.of()),
                    Map.of("phases", List.of()),
                    contentGraph
            );
        }
        DeterministicLoadOrder loadOrder = deterministicLoadOrder(scanResult, diagnostics);
        List<EchoNativeServiceDescriptor> services = serviceDescriptors(scanResult);
        return new EchoNativeGraphPlan(
                List.copyOf(diagnostics),
                loadOrder.modules(),
                moduleGraph(scanResult, loadOrder),
                featureGraph(scanResult),
                serviceGraph(services),
                lifecyclePlan(loadOrder.modules(), diagnostics),
                contentGraph
        );
    }

    private static Path contentGraphRoot() {
        String property = System.getProperty("echo.content.graph.root");
        if (property == null || property.isBlank()) {
            return null;
        }
        return Path.of(property);
    }

    private static List<String> moduleIds(EchoNativeScanResult scanResult) {
        return scanResult.descriptors().stream()
                .map(EchoNativeAddonDescriptor::id)
                .sorted()
                .distinct()
                .toList();
    }

    public List<String> loadOrder(EchoNativeScanResult scanResult) {
        if (scanResult.packProfile() == null) {
            return List.of();
        }
        return deterministicLoadOrder(scanResult, new ArrayList<>()).modules();
    }

    public List<EchoNativeServiceDescriptor> serviceDescriptors(EchoNativeScanResult scanResult) {
        return scanResult.descriptors().stream()
                .filter(descriptor -> !descriptor.provides().isEmpty())
                .sorted(Comparator.comparing(EchoNativeAddonDescriptor::id))
                .map(descriptor -> new EchoNativeServiceDescriptor(
                        descriptor.id() + ".service",
                        descriptor.id(),
                        "PLAN_SERVICES",
                        descriptor.provides()
                ))
                .toList();
    }

    private static DeterministicLoadOrder deterministicLoadOrder(
            EchoNativeScanResult scanResult,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, EchoNativeAddonDescriptor> descriptorsById = new TreeMap<>();
        for (EchoNativeAddonDescriptor descriptor : scanResult.descriptors()) {
            descriptorsById.putIfAbsent(descriptor.id(), descriptor);
        }

        Set<String> allModuleIds = new TreeSet<>();
        allModuleIds.addAll(scanResult.packProfile().requiredModules());
        allModuleIds.addAll(descriptorsById.keySet());

        Map<String, Set<String>> dependentsByDependency = new TreeMap<>();
        Map<String, Integer> incomingCounts = new TreeMap<>();
        for (String moduleId : allModuleIds) {
            dependentsByDependency.put(moduleId, new TreeSet<>());
            incomingCounts.put(moduleId, 0);
        }

        List<Map<String, Object>> missingReferences = new ArrayList<>();
        List<Map<String, Object>> dependencyEdges = new ArrayList<>();
        for (EchoNativeAddonDescriptor descriptor : descriptorsById.values()) {
            registerDependencies(
                    descriptor,
                    "requires",
                    descriptor.requires(),
                    true,
                    allModuleIds,
                    dependentsByDependency,
                    incomingCounts,
                    dependencyEdges,
                    missingReferences
            );
            registerDependencies(
                    descriptor,
                    "optional",
                    descriptor.optional(),
                    false,
                    allModuleIds,
                    dependentsByDependency,
                    incomingCounts,
                    dependencyEdges,
                    missingReferences
            );
        }

        Set<String> ready = new TreeSet<>();
        for (Map.Entry<String, Integer> entry : incomingCounts.entrySet()) {
            if (entry.getValue() == 0) {
                ready.add(entry.getKey());
            }
        }

        List<String> ordered = new ArrayList<>();
        while (!ready.isEmpty()) {
            String moduleId = ready.iterator().next();
            ready.remove(moduleId);
            ordered.add(moduleId);
            for (String dependent : dependentsByDependency.getOrDefault(moduleId, Set.of())) {
                int updated = incomingCounts.get(dependent) - 1;
                incomingCounts.put(dependent, updated);
                if (updated == 0) {
                    ready.add(dependent);
                }
            }
        }

        List<String> cycleModules = incomingCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
        if (!cycleModules.isEmpty()) {
            ordered.addAll(cycleModules);
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-DEPENDENCY-CYCLE",
                    EchoNativeIssueSeverity.ERROR,
                    "Dependency cycle detected",
                    "Native module dependencies contain a cycle: " + String.join(", ", cycleModules) + ".",
                    null,
                    scanResult.packProfile().id(),
                    List.of(path(scanResult.packProfile().profilePath())),
                    "Break the cycle so the Native Loader can compute a deterministic release load order."
            ));
        }
        for (Map<String, Object> missingReference : missingReferences) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-DEPENDENCY-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Required dependency missing",
                    "Module '" + missingReference.get("from") + "' requires missing module '"
                            + missingReference.get("to") + "'.",
                    String.valueOf(missingReference.get("from")),
                    scanResult.packProfile().id(),
                    List.of(path(scanResult.packProfile().profilePath())),
                    "Add the required module descriptor or remove the required dependency before release."
            ));
        }

        return new DeterministicLoadOrder(
                List.copyOf(new LinkedHashSet<>(ordered)),
                dependencyEdges,
                missingReferences,
                cycleModules
        );
    }

    private static void registerDependencies(
            EchoNativeAddonDescriptor descriptor,
            String kind,
            List<String> dependencyIds,
            boolean required,
            Set<String> allModuleIds,
            Map<String, Set<String>> dependentsByDependency,
            Map<String, Integer> incomingCounts,
            List<Map<String, Object>> dependencyEdges,
            List<Map<String, Object>> missingReferences
    ) {
        for (String dependencyId : sortedDependencyIds(dependencyIds)) {
            if (!allModuleIds.contains(dependencyId)) {
                if (required) {
                    missingReferences.add(edge(kind, descriptor.id(), dependencyId));
                }
                continue;
            }
            if (dependencyId.equals(descriptor.id())) {
                dependencyEdges.add(edge(kind, dependencyId, descriptor.id()));
                incomingCounts.put(descriptor.id(), incomingCounts.get(descriptor.id()) + 1);
                dependentsByDependency.computeIfAbsent(dependencyId, ignored -> new TreeSet<>()).add(descriptor.id());
                continue;
            }
            dependencyEdges.add(edge(kind, dependencyId, descriptor.id()));
            incomingCounts.put(descriptor.id(), incomingCounts.get(descriptor.id()) + 1);
            dependentsByDependency.computeIfAbsent(dependencyId, ignored -> new TreeSet<>()).add(descriptor.id());
        }
    }

    private static Set<String> sortedDependencyIds(List<String> dependencyIds) {
        Set<String> sorted = new TreeSet<>();
        if (dependencyIds == null) {
            return sorted;
        }
        for (String dependencyId : dependencyIds) {
            if (dependencyId != null && !dependencyId.isBlank()) {
                sorted.add(dependencyId);
            }
        }
        return sorted;
    }

    private static Map<String, Object> moduleGraph(EchoNativeScanResult scanResult, DeterministicLoadOrder loadOrder) {
        List<Map<String, Object>> nodes = scanResult.descriptors().stream()
                .sorted(Comparator.comparing(EchoNativeAddonDescriptor::id))
                .map(descriptor -> {
                    Map<String, Object> node = new LinkedHashMap<>();
                    node.put("apiStability", descriptor.apiStability().name());
                    node.put("id", descriptor.id());
                    node.put("kind", descriptor.kind());
                    node.put("loadIndex", loadOrder.modules().indexOf(descriptor.id()));
                    node.put("name", descriptor.name());
                    node.put("official", descriptor.official());
                    node.put("provides", descriptor.provides());
                    node.put("requires", descriptor.requires());
                    node.put("role", descriptor.role());
                    node.put("side", sideName(descriptor.side()));
                    node.put("standalone", descriptor.standalone());
                    node.put("trustLevel", descriptor.trustLevel().name());
                    node.put("version", descriptor.version());
                    return node;
                })
                .toList();

        List<Map<String, Object>> edges = new ArrayList<>();
        for (EchoNativeAddonDescriptor descriptor : scanResult.descriptors()) {
            descriptor.requires().forEach(required -> edges.add(edge("requires", descriptor.id(), required)));
            descriptor.optional().forEach(optional -> edges.add(edge("optional", descriptor.id(), optional)));
            descriptor.provides().forEach(feature -> edges.add(edge("provides_feature", descriptor.id(), feature)));
            descriptor.consumes().forEach(feature -> edges.add(edge("consumes_feature", descriptor.id(), feature)));
        }
        edges.sort(Comparator.comparing(edge -> edge.get("kind") + ":" + edge.get("from") + ":" + edge.get("to")));

        Map<String, Object> graph = new LinkedHashMap<>();
        graph.put("dependencyCycleDetected", !loadOrder.cycleModules().isEmpty());
        graph.put("dependencyCycleModules", loadOrder.cycleModules());
        graph.put("dependencyEdges", loadOrder.dependencyEdges());
        graph.put("dependencyMissingReferences", loadOrder.missingReferences());
        graph.put("dependencyOrderDeterministic",
                loadOrder.cycleModules().isEmpty() && loadOrder.missingReferences().isEmpty());
        graph.put("dependencyOrderPolicy", "topological_sort_required_then_present_optional_with_lexicographic_tie_breaks");
        graph.put("edgeCount", edges.size());
        graph.put("edges", edges);
        graph.put("moduleLoadOrder", loadOrder.modules());
        graph.put("nodeCount", nodes.size());
        graph.put("nodes", nodes);
        graph.put("phase", "native_dry_run");
        return graph;
    }

    private static Map<String, Object> featureGraph(EchoNativeScanResult scanResult) {
        Map<String, Set<String>> providers = new TreeMap<>();
        Map<String, Set<String>> consumers = new TreeMap<>();
        for (EchoNativeAddonDescriptor descriptor : scanResult.descriptors()) {
            descriptor.provides().forEach(feature -> providers.computeIfAbsent(feature, ignored -> new TreeSet<>()).add(descriptor.id()));
            descriptor.consumes().forEach(feature -> consumers.computeIfAbsent(feature, ignored -> new TreeSet<>()).add(descriptor.id()));
        }

        Set<String> allFeatures = new TreeSet<>();
        allFeatures.addAll(providers.keySet());
        allFeatures.addAll(consumers.keySet());
        if (scanResult.packProfile() != null) {
            allFeatures.addAll(scanResult.packProfile().requiredFeatures());
            allFeatures.addAll(scanResult.packProfile().optionalFeatures());
        }

        List<Map<String, Object>> features = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        for (String feature : allFeatures) {
            boolean required = scanResult.packProfile() != null && scanResult.packProfile().requiredFeatures().contains(feature);
            boolean optional = scanResult.packProfile() != null && scanResult.packProfile().optionalFeatures().contains(feature);
            List<String> featureProviders = List.copyOf(providers.getOrDefault(feature, Set.of()));
            List<String> featureConsumers = List.copyOf(consumers.getOrDefault(feature, Set.of()));
            String status = featureProviders.isEmpty()
                    ? required ? "missing_required" : optional ? "missing_optional" : "unprovided"
                    : "provided";
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("consumers", featureConsumers);
            item.put("featureId", feature);
            item.put("optionalForPack", optional);
            item.put("providers", featureProviders);
            item.put("requiredByPack", required);
            item.put("status", status);
            features.add(item);
            featureProviders.forEach(provider -> edges.add(edge("provides_feature", provider, feature)));
            featureConsumers.forEach(consumer -> edges.add(edge("consumes_feature", consumer, feature)));
        }
        edges.sort(Comparator.comparing(edge -> edge.get("kind") + ":" + edge.get("from") + ":" + edge.get("to")));

        Map<String, Object> graph = new LinkedHashMap<>();
        graph.put("edgeCount", edges.size());
        graph.put("edges", edges);
        graph.put("featureCount", features.size());
        graph.put("features", features);
        graph.put("missingOptional", features.stream().filter(feature -> "missing_optional".equals(feature.get("status"))).count());
        graph.put("missingRequired", features.stream().filter(feature -> "missing_required".equals(feature.get("status"))).count());
        graph.put("phase", "native_dry_run");
        return graph;
    }

    private static Map<String, Object> serviceGraph(List<EchoNativeServiceDescriptor> services) {
        Map<String, Object> graph = new LinkedHashMap<>();
        graph.put("phase", "native_dry_run");
        graph.put("serviceCount", services.size());
        graph.put("services", services.stream().map(service -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", service.id());
            item.put("lifecyclePhase", service.lifecyclePhase());
            item.put("providedFeatures", service.providedFeatures());
            item.put("providerModule", service.providerModule());
            return item;
        }).toList());
        return graph;
    }

    private static Map<String, Object> lifecyclePlan(List<String> loadOrder, List<EchoNativeDiagnostic> diagnostics) {
        List<Map<String, Object>> phases = new ArrayList<>();
        phases.add(phase("DISCOVER", loadOrder, "Read fixture descriptors only."));
        phases.add(phase("VALIDATE", loadOrder, "Validate descriptors, pack profile, required modules, and required features."));
        phases.add(phase("PLAN_SERVICES", loadOrder, "Create service descriptors without executing services."));
        phases.add(phase("PLAN_BOOTSTRAP", loadOrder, "Plan classpath, native libraries, launch arguments, and access policy."));
        phases.add(phase(
                diagnostics.stream().anyMatch(diagnostic -> diagnostic.severity().name().equals("ERROR") || diagnostic.severity().name().equals("FATAL"))
                        ? "BLOCKED"
                        : "READY_FOR_PRODUCT_LAUNCH_CHECK",
                loadOrder,
                "Native dry-run planning is complete; use the product launcher gates for release readiness."
        ));
        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("dryRunOnly", true);
        plan.put("launchBlocked", true);
        plan.put("phases", phases);
        return plan;
    }

    private static Map<String, Object> phase(String id, List<String> loadOrder, String summary) {
        Map<String, Object> phase = new LinkedHashMap<>();
        phase.put("id", id);
        phase.put("modules", loadOrder);
        phase.put("summary", summary);
        return phase;
    }

    private static Map<String, Object> edge(String kind, String from, String to) {
        Map<String, Object> edge = new LinkedHashMap<>();
        edge.put("from", from);
        edge.put("kind", kind);
        edge.put("to", to);
        return edge;
    }

    private static String sideName(EchoNativeRuntimeSide side) {
        return (side == null ? EchoNativeRuntimeSide.UNKNOWN : side).name();
    }

    private static String path(Path path) {
        if (path == null) {
            return "";
        }
        Path normalized = path.toAbsolutePath().normalize();
        Path root = Path.of("").toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString().replace('\\', '/');
        }
        Path workspaceRoot = root.getParent();
        if (workspaceRoot != null && normalized.startsWith(workspaceRoot)) {
            return workspaceRoot.relativize(normalized).toString().replace('\\', '/');
        }
        Path fileName = normalized.getFileName();
        return fileName == null ? "" : fileName.toString().replace('\\', '/');
    }

    private record DeterministicLoadOrder(
            List<String> modules,
            List<Map<String, Object>> dependencyEdges,
            List<Map<String, Object>> missingReferences,
            List<String> cycleModules
    ) {
    }
}
