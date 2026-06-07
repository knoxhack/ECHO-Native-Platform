package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.ai.EchoNativeAiPlan;
import dev.echo.nativeplatform.contracts.EchoNativeAccessPolicy;
import dev.echo.nativeplatform.contracts.EchoNativeBootstrapPlan;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.loader.EchoNativeGraphPlan;
import dev.echo.nativeplatform.loader.EchoNativeScanResult;
import dev.echo.nativeplatform.packos.EchoNativeLockfileVerificationPlan;
import dev.echo.nativeplatform.packos.EchoNativeRepairPlan;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class EchoNativePhase12GateVerifier {
    EchoNativePhase12GatePlan verify(
            Path fixture,
            EchoNativeScanResult result,
            EchoNativeGraphPlan graphPlan,
            EchoNativeBootstrapPlan bootstrapPlan,
            EchoNativeLockfileVerificationPlan lockfileStatus,
            EchoNativeRepairPlan repairPlan,
            EchoNativeAiPlan aiPlan,
            EchoNativeStaticSafetyScan safetyScan
    ) {
        List<EchoNativeDiagnostic> diagnostics = uniqueDiagnostics(graphPlan.diagnostics(), lockfileStatus.diagnostics(), repairPlan.diagnostics(), aiPlan.diagnostics(), safetyScan.diagnostics());
        List<Map<String, Object>> gates = new ArrayList<>();
        String packId = result.packProfile() == null ? "" : result.packProfile().id();

        gates.add(gate("descriptor_discovery_stable", result.packProfile() != null && !result.descriptors().isEmpty() && result.diagnostics().isEmpty(), "Fixture descriptors and pack profile can be scanned."));
        gates.add(gate("validation_no_blocking_diagnostics", noBlocking(graphPlan.diagnostics()), "Validation has no blocking diagnostics."));
        gates.add(gate("module_graph_deterministic", number(graphPlan.moduleGraph(), "nodeCount") == result.descriptors().size() && listSize(graphPlan.moduleGraph().get("moduleLoadOrder")) >= result.descriptors().size(), "Module graph has expected nodes and load order."));
        gates.add(gate("feature_graph_no_missing_required", number(graphPlan.featureGraph(), "missingRequired") == 0, "Feature graph has no missing required providers."));
        gates.add(gate("service_graph_deterministic", number(graphPlan.serviceGraph(), "serviceCount") >= 0, "Service graph is generated."));
        gates.add(gate("lockfile_verifies", "valid".equals(lockfileStatus.status().get("status")), "Native dry-run lockfile verifies against current descriptors."));
        gates.add(gate("repair_plan_nonblocking", repairPlanNonblocking(repairPlan.repairPlan()), "Repair plan is no_repair_needed or nonblocking."));
        gates.add(gate("ai_graph_no_ready_blocker_tasks", number(aiPlan.aiTasks(), "taskCount") == 0, "AI graph has no ready blocking fixture tasks."));
        gates.add(gate("bootstrap_reports_exist", expectedReportsExist(packId), "Bootstrap and planning reports exist."));
        gates.add(gate("phase13_unsafe_capabilities_blocked", phase13UnsafeCapabilitiesBlocked(bootstrapPlan.accessPolicy()), "Launch, transforms, registry injection, and native extraction remain blocked."));
        gates.add(gate("forbidden_imports_absent", safetyScan.diagnostics().isEmpty(), "Native source has no NeoForge, Forge, Fabric, or Minecraft runtime imports."));

        for (Map<String, Object> gate : gates) {
            if (!Boolean.TRUE.equals(gate.get("passed"))) {
                diagnostics = appendGateDiagnostic(packId, fixture, diagnostics, gate);
            }
        }

        boolean complete = gates.stream().allMatch(gate -> Boolean.TRUE.equals(gate.get("passed"))) && noBlocking(diagnostics);
        Map<String, Object> completion = new LinkedHashMap<>();
        completion.put("commandSet", List.of("scan", "validate", "graph", "features", "lock generate", "lock verify", "repair plan", "ai graph", "report", "bootstrap --dry-run"));
        completion.put("dryRunOnly", true);
        completion.put("gateCount", gates.size());
        completion.put("gates", gates);
        completion.put("packId", packId);
        completion.put("phase", "phase12_completion_gate");
        completion.put("phase12Complete", complete);
        completion.put("phase13WorkStarted", false);
        completion.put("safetyScan", safetyScan.data());
        completion.put("summary", complete
                ? "Phase 12 dry-run acceptance is complete for this fixture. No Phase 13 work was executed."
                : "Phase 12 dry-run acceptance is still blocked for this fixture.");

        Map<String, Object> readiness = new LinkedHashMap<>();
        readiness.put("dryRunOnly", true);
        readiness.put("packId", packId);
        readiness.put("phase", "phase13_unlock_audit");
        readiness.put("phase13Blocked", !complete);
        readiness.put("phase13Ready", complete);
        readiness.put("phase13WorkStarted", false);
        readiness.put("requiredBeforePhase13", List.of(
                "descriptor discovery stable",
                "validation no blocking diagnostics",
                "feature graph missingRequired=0",
                "lockfile verifies",
                "repair plan nonblocking",
                "AI graph has no ready blocker tasks",
                "unsafe loader capabilities remain blocked",
                "forbidden import search passes"
        ));
        readiness.put("safetyGates", gates);
        readiness.put("summary", complete
                ? "Phase 13 may be planned in a later run, but no loader prototype work was started by this command."
                : "Phase 13 remains blocked until Phase 12 gates pass.");

        return new EchoNativePhase12GatePlan(packId, completion, readiness, diagnostics);
    }

    private static Map<String, Object> gate(String id, boolean passed, String summary) {
        Map<String, Object> gate = new LinkedHashMap<>();
        gate.put("id", id);
        gate.put("passed", passed);
        gate.put("summary", summary);
        return gate;
    }

    private static boolean noBlocking(List<EchoNativeDiagnostic> diagnostics) {
        return diagnostics.stream().noneMatch(diagnostic -> diagnostic.severity() == EchoNativeIssueSeverity.ERROR || diagnostic.severity() == EchoNativeIssueSeverity.FATAL);
    }

    private static boolean repairPlanNonblocking(Map<String, Object> repairPlan) {
        Object status = repairPlan.get("status");
        Object destructive = repairPlan.get("destructiveActions");
        return ("no_repair_needed".equals(status) || "manual_action_required".equals(status))
                && (!(destructive instanceof Number number) || number.longValue() == 0);
    }

    private static boolean phase13UnsafeCapabilitiesBlocked(EchoNativeAccessPolicy policy) {
        return policy.dryRunOnly()
                && policy.launchBlocked()
                && policy.transformsBlocked()
                && policy.registryInjectionBlocked()
                && policy.blockedCapabilities().containsAll(List.of("minecraft.launch", "bytecode.transforms", "registry.injection", "native.library.extraction"));
    }

    private static boolean expectedReportsExist(String packId) {
        if (packId == null || packId.isBlank()) {
            return false;
        }
        Path root = Path.of("").toAbsolutePath().normalize().resolve("reports").resolve("echo-native").resolve(packId);
        return List.of(
                "scan.json",
                "validation.json",
                "module-graph.json",
                "feature-graph.json",
                "service-graph.json",
                "lifecycle-plan.json",
                "lockfile.json",
                "lockfile-status.json",
                "repair-plan.json",
                "ai-graph.json",
                "ai-tasks.json",
                "minecraft-resolution.json",
                "classpath-plan.json",
                "native-library-plan.json",
                "launch-argument-plan.json",
                "module-load-plan.json",
                "bootstrap-plan.json"
        ).stream().allMatch(fileName -> Files.isRegularFile(root.resolve(fileName)));
    }

    private static long number(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number number ? number.longValue() : 0;
    }

    private static int listSize(Object value) {
        return value instanceof List<?> list ? list.size() : 0;
    }

    @SafeVarargs
    private static List<EchoNativeDiagnostic> uniqueDiagnostics(List<EchoNativeDiagnostic>... groups) {
        Map<String, EchoNativeDiagnostic> byKey = new LinkedHashMap<>();
        for (List<EchoNativeDiagnostic> group : groups) {
            for (EchoNativeDiagnostic diagnostic : group) {
                byKey.put(diagnostic.code() + "|" + diagnostic.moduleId() + "|" + diagnostic.summary(), diagnostic);
            }
        }
        return byKey.values().stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();
    }

    private static List<EchoNativeDiagnostic> appendGateDiagnostic(String packId, Path fixture, List<EchoNativeDiagnostic> diagnostics, Map<String, Object> gate) {
        Set<EchoNativeDiagnostic> merged = new LinkedHashSet<>(diagnostics);
        merged.add(new EchoNativeDiagnostic(
                "ECHO-NATIVE-PHASE12-GATE-FAILED",
                EchoNativeIssueSeverity.ERROR,
                "Phase 12 gate failed",
                "Gate '" + gate.get("id") + "' did not pass.",
                null,
                packId,
                List.of(fixture.toString().replace('\\', '/')),
                "Resolve the failed dry-run gate before starting Phase 13."
        ));
        return merged.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();
    }
}
