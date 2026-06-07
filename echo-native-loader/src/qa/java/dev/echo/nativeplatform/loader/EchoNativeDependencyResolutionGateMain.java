package dev.echo.nativeplatform.loader;

import dev.echo.nativeplatform.contracts.EchoNativeAddonDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeApiStability;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativePackProfile;
import dev.echo.nativeplatform.contracts.EchoNativeRuntimeSide;
import dev.echo.nativeplatform.contracts.EchoNativeTrustLevel;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class EchoNativeDependencyResolutionGateMain {
    private EchoNativeDependencyResolutionGateMain() {
    }

    public static void main(String[] args) {
        requireDeterministicTopologicalOrder();
        requireMissingDependencyEvidence();
        requireCycleEvidence();
        System.out.println("native dependency resolution gate PASS");
    }

    private static void requireDeterministicTopologicalOrder() {
        EchoNativeGraphPlan plan = new EchoNativeGraphPlanner().plan(scan(
                "echo_c",
                List.of(
                        descriptor("echo_c", List.of("echo_a"), List.of("echo_b")),
                        descriptor("echo_a", List.of("echo_b"), List.of()),
                        descriptor("echo_b", List.of(), List.of())
                )
        ));
        require(plan.moduleLoadOrder().equals(List.of("echo_b", "echo_a", "echo_c")),
                "Required and present optional dependencies must load before dependents: " + plan.moduleLoadOrder());
        Map<String, Object> graph = plan.moduleGraph();
        require(Boolean.TRUE.equals(graph.get("dependencyOrderDeterministic")),
                "Acyclic graph with present dependencies must be deterministic: " + graph);
        require(Boolean.FALSE.equals(graph.get("dependencyCycleDetected")),
                "Acyclic dependency graph must not report a cycle: " + graph);
    }

    private static void requireMissingDependencyEvidence() {
        EchoNativeGraphPlan plan = new EchoNativeGraphPlanner().plan(scan(
                "echo_a",
                List.of(descriptor("echo_a", List.of("missing_dependency"), List.of()))
        ));
        Map<String, Object> graph = plan.moduleGraph();
        require(Boolean.FALSE.equals(graph.get("dependencyOrderDeterministic")),
                "Missing required dependency must block deterministic release proof: " + graph);
        require(list(graph, "dependencyMissingReferences").stream()
                        .map(EchoNativeDependencyResolutionGateMain::object)
                        .anyMatch(edge -> "echo_a".equals(edge.get("from"))
                                && "missing_dependency".equals(edge.get("to"))),
                "Missing dependency report must identify the concrete module edge: " + graph);
        require(plan.diagnostics().stream().map(EchoNativeDiagnostic::code)
                        .anyMatch("ECHO-NATIVE-DEPENDENCY-MISSING"::equals),
                "Missing required dependency must produce a release-blocking diagnostic.");
    }

    private static void requireCycleEvidence() {
        EchoNativeGraphPlan plan = new EchoNativeGraphPlanner().plan(scan(
                "echo_a",
                List.of(
                        descriptor("echo_a", List.of("echo_b"), List.of()),
                        descriptor("echo_b", List.of("echo_a"), List.of())
                )
        ));
        Map<String, Object> graph = plan.moduleGraph();
        require(Boolean.TRUE.equals(graph.get("dependencyCycleDetected")),
                "Dependency cycle must be detected: " + graph);
        require(list(graph, "dependencyCycleModules").containsAll(List.of("echo_a", "echo_b")),
                "Dependency cycle report must name every module in the cycle: " + graph);
        require(plan.diagnostics().stream().map(EchoNativeDiagnostic::code)
                        .anyMatch("ECHO-NATIVE-DEPENDENCY-CYCLE"::equals),
                "Dependency cycle must produce a release-blocking diagnostic.");
    }

    private static EchoNativeScanResult scan(String rootModule, List<EchoNativeAddonDescriptor> descriptors) {
        EchoNativePackProfile profile = new EchoNativePackProfile(
                "echo.pack.v1",
                "dependency-gate-pack",
                "Dependency Gate Pack",
                "qa",
                rootModule,
                "1.21.1",
                "echo-native-loader",
                "0.1.0-native-beta",
                List.of(rootModule),
                List.of(),
                List.of(),
                Path.of("qa/dependency-gate/echo.pack.json")
        );
        return new EchoNativeScanResult(profile, descriptors, List.of());
    }

    private static EchoNativeAddonDescriptor descriptor(String id, List<String> requires, List<String> optional) {
        return new EchoNativeAddonDescriptor(
                "echo.mod.v1",
                id,
                id,
                "1.0.0",
                "addon",
                "runtime",
                "qa." + id + ".Entrypoint",
                EchoNativeRuntimeSide.COMMON,
                EchoNativeTrustLevel.LOCAL,
                EchoNativeApiStability.BETA,
                false,
                true,
                requires,
                optional,
                List.of(),
                List.of(),
                List.of(),
                Map.<String, Object>of("nativeClasspath", List.of("qa/" + id + ".jar")),
                Path.of("qa/dependency-gate/" + id + "/META-INF/echo.mod.json")
        );
    }

    private static List<?> list(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List<?> list) {
            return list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
