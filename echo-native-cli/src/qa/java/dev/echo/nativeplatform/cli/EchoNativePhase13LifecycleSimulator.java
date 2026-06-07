package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeCrashBoundaryResult;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativeLifecycleSimulationResult;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoNativePhase13LifecycleSimulator {
    EchoNativePhase13LifecycleSimulationOutcome simulate(
            String packId,
            Path fixture,
            Path phase13PlanPath,
            Path lifecyclePlanPath
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Object> phase13Plan = readRequiredReport(phase13PlanPath, fixture, packId, "ECHO-NATIVE-PHASE13-PLAN-MISSING", "Phase 13 plan report missing", diagnostics);
        Map<String, Object> lifecyclePlan = readRequiredReport(lifecyclePlanPath, fixture, packId, "ECHO-NATIVE-LIFECYCLE-PLAN-MISSING", "Lifecycle simulation plan missing", diagnostics);

        Map<String, Object> phase13Data = EchoNativeJson.asObject(phase13Plan.get("data"));
        Map<String, Object> lifecycleData = EchoNativeJson.asObject(lifecyclePlan.get("data"));
        if (!phase13Plan.isEmpty()) {
            boolean pass = "PASS".equals(phase13Plan.get("status"));
            boolean planningStarted = Boolean.TRUE.equals(phase13Data.get("phase13PlanningStarted"));
            boolean planOnly = Boolean.TRUE.equals(phase13Data.get("planOnly"));
            boolean prototypeRuntimeStarted = Boolean.TRUE.equals(phase13Data.get("prototypeRuntimeStarted"));
            boolean unsafeRuntimeWorkStarted = Boolean.TRUE.equals(phase13Data.get("unsafeRuntimeWorkStarted"));
            if (!pass || !planningStarted || !planOnly || prototypeRuntimeStarted || unsafeRuntimeWorkStarted) {
                diagnostics.add(new EchoNativeDiagnostic(
                        "ECHO-NATIVE-PHASE13-PLAN-BLOCKED",
                        EchoNativeIssueSeverity.ERROR,
                        "Phase 13 plan not ready for lifecycle simulation",
                        "Lifecycle simulation requires a PASS phase13-plan.json with planning enabled and no runtime work started.",
                        null,
                        packId,
                        List.of(relativeReportPath(phase13PlanPath)),
                        "Run echo-native phase13 plan for a fixture with PASS Phase 13 readiness first."
                ));
            }
            diagnostics.addAll(reportDiagnostics(phase13Plan, packId));
        }

        if (!lifecyclePlan.isEmpty() && !"PASS".equals(lifecyclePlan.get("status"))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-LIFECYCLE-PLAN-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Lifecycle simulation plan is not PASS",
                    "The simulator only runs from a PASS lifecycle-simulation-plan.json.",
                    null,
                    packId,
                    List.of(relativeReportPath(lifecyclePlanPath)),
                    "Fix Phase 13 planning diagnostics before simulation."
            ));
            diagnostics.addAll(reportDiagnostics(lifecyclePlan, packId));
        }

        diagnostics = unique(diagnostics);
        boolean canSimulate = diagnostics.isEmpty();
        List<String> phases = stringList(lifecycleData.get("lifecyclePhases"));
        if (phases.isEmpty()) {
            phases = List.of("DISCOVER_DESCRIPTORS", "VALIDATE_CONTRACTS", "SIMULATE_LOAD_ORDER", "SIMULATE_SERVICE_ATTACH", "SIMULATE_SHUTDOWN_BOUNDARY");
        }
        List<String> modules = stringList(lifecycleData.get("moduleLoadOrder"));
        EchoNativeLifecycleSimulationResult lifecycleResult = new EchoNativeLifecycleSimulationResult(
                "phase13.lifecycle.simulation.result",
                canSimulate,
                false,
                false,
                false,
                canSimulate ? phases : List.of(),
                canSimulate ? modules : List.of()
        );
        EchoNativeCrashBoundaryResult crashResult = new EchoNativeCrashBoundaryResult(
                "phase13.crash.boundary.result",
                true,
                true,
                false,
                false,
                0,
                canSimulate ? phases : List.of()
        );

        return new EchoNativePhase13LifecycleSimulationOutcome(
                packId,
                lifecycleResult(packId, lifecycleResult, diagnostics, phases, modules),
                crashBoundaryResult(packId, crashResult, diagnostics),
                diagnostics
        );
    }

    private static Map<String, Object> readRequiredReport(
            Path reportPath,
            Path fixture,
            String packId,
            String missingCode,
            String missingTitle,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        if (!Files.isRegularFile(reportPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    missingCode,
                    EchoNativeIssueSeverity.ERROR,
                    missingTitle,
                    "Required Phase 13 simulation input report was not found.",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate Phase 13 planning reports before lifecycle simulation."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
    }

    private static List<EchoNativeDiagnostic> reportDiagnostics(Map<String, Object> report, String packId) {
        Object issues = report.get("issues");
        if (!(issues instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> EchoNativeJson.asObject(item))
                .sorted(Comparator.comparing(item -> String.valueOf(item.get("code")) + ":" + item.get("summary")))
                .map(item -> new EchoNativeDiagnostic(
                        String.valueOf(item.getOrDefault("code", "ECHO-NATIVE-UPSTREAM-DIAGNOSTIC")),
                        EchoNativeIssueSeverity.ERROR,
                        String.valueOf(item.getOrDefault("title", "Upstream diagnostic")),
                        String.valueOf(item.getOrDefault("summary", "Upstream Phase 13 report is not PASS.")),
                        item.get("moduleId") == null ? null : String.valueOf(item.get("moduleId")),
                        packId,
                        EchoNativeJson.stringList(item.get("likelyFiles")),
                        String.valueOf(item.getOrDefault("suggestedFix", "Resolve upstream diagnostics first."))
                ))
                .toList();
    }

    private static Map<String, Object> lifecycleResult(
            String packId,
            EchoNativeLifecycleSimulationResult result,
            List<EchoNativeDiagnostic> diagnostics,
            List<String> plannedPhases,
            List<String> plannedModules
    ) {
        Map<String, Object> data = base("phase13_lifecycle_simulation_result", diagnostics);
        data.put("classloaderCreated", result.classloaderCreated());
        data.put("completedPhases", result.completedPhases());
        data.put("executedAddonCode", result.executedAddonCode());
        data.put("moduleCount", plannedModules.size());
        data.put("packId", packId);
        data.put("plannedPhases", plannedPhases);
        data.put("processLaunched", result.processLaunched());
        data.put("simulated", result.simulated());
        data.put("simulatedModules", result.simulatedModules());
        data.put("simulationId", result.simulationId());
        data.put("summary", result.simulated()
                ? "Lifecycle phases were simulated in memory without loading addon or game classes."
                : "Lifecycle simulation was blocked by upstream Phase 13 diagnostics.");
        return data;
    }

    private static Map<String, Object> crashBoundaryResult(
            String packId,
            EchoNativeCrashBoundaryResult result,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_crash_boundary_result", diagnostics);
        data.put("boundaryId", result.boundaryId());
        data.put("capturedDiagnostics", result.capturedDiagnostics());
        data.put("crashBoundaryActive", result.crashBoundaryActive());
        data.put("mutatedState", result.mutatedState());
        data.put("packId", packId);
        data.put("protectedPhases", result.protectedPhases());
        data.put("simulatedCrashCount", result.simulatedCrashCount());
        data.put("terminatedProcess", result.terminatedProcess());
        return data;
    }

    private static Map<String, Object> base(String phase, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("diagnosticCount", diagnostics.size());
        data.put("dryRunOnly", true);
        data.put("phase", phase);
        data.put("simulationOnly", true);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (item != null) {
                result.add(String.valueOf(item));
            }
        }
        return List.copyOf(result);
    }

    private static List<EchoNativeDiagnostic> unique(List<EchoNativeDiagnostic> diagnostics) {
        Map<String, EchoNativeDiagnostic> byKey = new LinkedHashMap<>();
        for (EchoNativeDiagnostic diagnostic : diagnostics) {
            byKey.put(diagnostic.code() + "|" + diagnostic.moduleId() + "|" + diagnostic.summary(), diagnostic);
        }
        return byKey.values().stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();
    }

    private static String relativeReportPath(Path path) {
        Path root = Path.of("").toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(root)) {
            return root.relativize(normalized).toString().replace('\\', '/');
        }
        return path.toString().replace('\\', '/');
    }
}
