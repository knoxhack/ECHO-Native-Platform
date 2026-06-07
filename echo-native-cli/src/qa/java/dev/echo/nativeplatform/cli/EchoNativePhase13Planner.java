package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeClassloaderBoundaryPlan;
import dev.echo.nativeplatform.contracts.EchoNativeControlledExperimentDescriptor;
import dev.echo.nativeplatform.contracts.EchoNativeCrashBoundaryPlan;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativeLoaderBoundaryPlan;
import dev.echo.nativeplatform.contracts.EchoNativeLifecycleSimulationPlan;
import dev.echo.nativeplatform.contracts.EchoNativeTestProcessPlan;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;
import dev.echo.nativeplatform.loader.EchoNativeGraphPlan;
import dev.echo.nativeplatform.loader.EchoNativeScanResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoNativePhase13Planner {
    EchoNativePhase13PrototypePlan plan(Path fixture, Path readinessPath, EchoNativeScanResult result, EchoNativeGraphPlan graphPlan) throws IOException {
        String packId = result.packProfile() == null ? fixture.getFileName().toString() : result.packProfile().id();
        List<EchoNativeDiagnostic> diagnostics = readinessDiagnostics(fixture, readinessPath, packId);
        List<String> moduleLoadOrder = graphPlan.moduleLoadOrder();
        if (moduleLoadOrder.isEmpty()) {
            moduleLoadOrder = result.descriptors().stream()
                    .map(descriptor -> descriptor.id())
                    .sorted()
                    .toList();
        }

        boolean ready = diagnostics.isEmpty();
        EchoNativeControlledExperimentDescriptor experiment = new EchoNativeControlledExperimentDescriptor(
                "phase13.m1.controlled_prototype_foundation",
                "phase13_m1",
                ready
                        ? "Plan controlled Native Loader prototype boundaries from Phase 12 reports."
                        : "Phase 13 prototype planning is blocked until Phase 12 readiness passes.",
                true,
                ready,
                ready ? List.of("lifecycle.simulation.plan", "boundary.plan", "crash.boundary.plan", "test.process.plan") : List.of(),
                blockedPrototypeCapabilities()
        );
        EchoNativeLoaderBoundaryPlan loaderBoundary = new EchoNativeLoaderBoundaryPlan(
                "phase13.loader.boundary",
                "Phase 13 M1 may define loader boundaries, but cannot create a production classloader or launch the game.",
                false,
                false,
                List.of("phase12-completion.json", "phase13-readiness.json", "module-load-plan.json", "service-graph.json"),
                List.of("phase13-plan.json", "classloader-boundary-plan.json", "lifecycle-simulation-plan.json", "crash-boundary-plan.json", "test-process-plan.json"),
                blockedPrototypeCapabilities()
        );
        EchoNativeLifecycleSimulationPlan lifecyclePlan = new EchoNativeLifecycleSimulationPlan(
                "phase13.lifecycle.simulation",
                List.of("DISCOVER_DESCRIPTORS", "VALIDATE_CONTRACTS", "SIMULATE_LOAD_ORDER", "SIMULATE_SERVICE_ATTACH", "SIMULATE_SHUTDOWN_BOUNDARY"),
                moduleLoadOrder,
                false,
                true
        );
        EchoNativeClassloaderBoundaryPlan classloaderPlan = new EchoNativeClassloaderBoundaryPlan(
                "phase13.classloader.boundary",
                false,
                false,
                false,
                List.of("parent_boundary", "addon_descriptor_boundary", "service_api_boundary", "resource_lookup_boundary"),
                List.of("production.classloader", "runtime.class.resolution", "bytecode.transforms", "game.runtime.linkage")
        );
        EchoNativeCrashBoundaryPlan crashBoundary = new EchoNativeCrashBoundaryPlan(
                "phase13.crash.boundary",
                true,
                false,
                false,
                List.of("descriptor_parse", "lifecycle_simulation", "service_simulation", "test_process_planning"),
                List.of("diagnostics.json", "phase13-plan.json", "crash-boundary-plan.json")
        );
        EchoNativeTestProcessPlan testProcessPlan = new EchoNativeTestProcessPlan(
                "phase13.test_process.plan",
                false,
                false,
                false,
                List.of("future.isolated.java.harness"),
                List.of("game.launch", "installed.pack.mutation", "native.library.extraction", "network.download", "registry.injection")
        );

        return new EchoNativePhase13PrototypePlan(
                packId,
                phase13Plan(packId, experiment, loaderBoundary, diagnostics),
                lifecycleSimulationPlan(packId, lifecyclePlan, diagnostics),
                classloaderBoundaryPlan(packId, classloaderPlan, diagnostics),
                crashBoundaryPlan(packId, crashBoundary, diagnostics),
                testProcessPlan(packId, testProcessPlan, diagnostics),
                diagnostics
        );
    }

    private static List<EchoNativeDiagnostic> readinessDiagnostics(Path fixture, Path readinessPath, String packId) throws IOException {
        if (!Files.isRegularFile(readinessPath)) {
            return List.of(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PHASE13-READINESS-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 readiness report missing",
                    "Phase 13 planning requires a PASS phase13-readiness.json from phase12 verify.",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Run echo-native phase12 verify for this fixture before phase13 plan."
            ));
        }

        Map<String, Object> envelope = EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(readinessPath)));
        Map<String, Object> data = EchoNativeJson.asObject(envelope.get("data"));
        boolean pass = "PASS".equals(envelope.get("status"));
        boolean ready = Boolean.TRUE.equals(data.get("phase13Ready"));
        boolean blocked = Boolean.TRUE.equals(data.get("phase13Blocked"));
        boolean phase13AlreadyStarted = Boolean.TRUE.equals(data.get("phase13WorkStarted"));
        if (pass && ready && !blocked && !phase13AlreadyStarted) {
            return List.of();
        }

        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        diagnostics.add(new EchoNativeDiagnostic(
                "ECHO-NATIVE-PHASE13-READINESS-BLOCKED",
                EchoNativeIssueSeverity.ERROR,
                "Phase 13 readiness gate blocked",
                "Phase 13 planning requires status PASS, phase13Ready=true, phase13Blocked=false, and phase13WorkStarted=false.",
                null,
                packId,
                List.of(relativeReportPath(readinessPath)),
                "Fix Phase 12 dry-run blockers, then rerun echo-native phase12 verify."
        ));
        Object issues = envelope.get("issues");
        if (issues instanceof List<?> list) {
            list.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> EchoNativeJson.asObject(item))
                    .sorted(Comparator.comparing(item -> String.valueOf(item.get("code")) + ":" + item.get("summary")))
                    .forEach(item -> diagnostics.add(new EchoNativeDiagnostic(
                            String.valueOf(item.getOrDefault("code", "ECHO-NATIVE-PHASE12-BLOCKER")),
                            EchoNativeIssueSeverity.ERROR,
                            String.valueOf(item.getOrDefault("title", "Phase 12 blocker")),
                            String.valueOf(item.getOrDefault("summary", "Phase 12 readiness did not pass.")),
                            item.get("moduleId") == null ? null : String.valueOf(item.get("moduleId")),
                            packId,
                            EchoNativeJson.stringList(item.get("likelyFiles")),
                            String.valueOf(item.getOrDefault("suggestedFix", "Resolve Phase 12 readiness blockers first."))
                    )));
        }
        return diagnostics;
    }

    private static Map<String, Object> phase13Plan(
            String packId,
            EchoNativeControlledExperimentDescriptor experiment,
            EchoNativeLoaderBoundaryPlan loaderBoundary,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m1_controlled_prototype_foundation", diagnostics);
        data.put("packId", packId);
        data.put("experiment", experimentMap(experiment));
        data.put("loaderBoundary", loaderBoundaryMap(loaderBoundary));
        data.put("phase13PlanningStarted", diagnostics.isEmpty());
        data.put("prototypeRuntimeStarted", false);
        data.put("summary", diagnostics.isEmpty()
                ? "Phase 13 M1 planning is unlocked for controlled prototype boundaries only."
                : "Phase 13 M1 planning is blocked by Phase 12 readiness diagnostics.");
        return data;
    }

    private static Map<String, Object> lifecycleSimulationPlan(
            String packId,
            EchoNativeLifecycleSimulationPlan lifecyclePlan,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_lifecycle_simulation_plan", diagnostics);
        data.put("executesAddonCode", lifecyclePlan.executesAddonCode());
        data.put("lifecyclePhases", lifecyclePlan.lifecyclePhases());
        data.put("moduleLoadOrder", lifecyclePlan.moduleLoadOrder());
        data.put("packId", packId);
        data.put("simulationId", lifecyclePlan.simulationId());
        return data;
    }

    private static Map<String, Object> classloaderBoundaryPlan(
            String packId,
            EchoNativeClassloaderBoundaryPlan classloaderPlan,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_classloader_boundary_plan", diagnostics);
        data.put("blockedCapabilities", classloaderPlan.blockedCapabilities());
        data.put("classloaderCreated", classloaderPlan.classloaderCreated());
        data.put("packId", packId);
        data.put("plannedBoundaries", classloaderPlan.plannedBoundaries());
        data.put("planId", classloaderPlan.planId());
        data.put("productionClassloader", classloaderPlan.productionClassloader());
        data.put("resolvesRuntimeClasses", classloaderPlan.resolvesRuntimeClasses());
        return data;
    }

    private static Map<String, Object> crashBoundaryPlan(
            String packId,
            EchoNativeCrashBoundaryPlan crashBoundary,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_crash_boundary_plan", diagnostics);
        data.put("boundaries", crashBoundary.boundaries());
        data.put("boundaryId", crashBoundary.boundaryId());
        data.put("capturesDiagnostics", crashBoundary.capturesDiagnostics());
        data.put("mutatesState", crashBoundary.mutatesState());
        data.put("packId", packId);
        data.put("recoveryReports", crashBoundary.recoveryReports());
        data.put("terminatesProcess", crashBoundary.terminatesProcess());
        return data;
    }

    private static Map<String, Object> testProcessPlan(
            String packId,
            EchoNativeTestProcessPlan testProcess,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_test_process_plan", diagnostics);
        data.put("allowedTargets", testProcess.allowedTargets());
        data.put("blockedTargets", testProcess.blockedTargets());
        data.put("gameLaunchAllowed", testProcess.gameLaunchAllowed());
        data.put("packId", packId);
        data.put("planId", testProcess.planId());
        data.put("processLaunchAllowed", testProcess.processLaunchAllowed());
        data.put("subprocessCreated", testProcess.subprocessCreated());
        return data;
    }

    private static Map<String, Object> base(String phase, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("blockedCapabilities", blockedPrototypeCapabilities());
        data.put("diagnosticCount", diagnostics.size());
        data.put("dryRunOnly", true);
        data.put("phase", phase);
        data.put("planOnly", true);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
    }

    private static Map<String, Object> experimentMap(EchoNativeControlledExperimentDescriptor experiment) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("allowedOperations", experiment.allowedOperations());
        data.put("blockedOperations", experiment.blockedOperations());
        data.put("dryRunOnly", experiment.dryRunOnly());
        data.put("enabled", experiment.enabled());
        data.put("id", experiment.id());
        data.put("phase", experiment.phase());
        data.put("summary", experiment.summary());
        return data;
    }

    private static Map<String, Object> loaderBoundaryMap(EchoNativeLoaderBoundaryPlan loaderBoundary) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("blockedCapabilities", loaderBoundary.blockedCapabilities());
        data.put("boundaryId", loaderBoundary.boundaryId());
        data.put("inputs", loaderBoundary.inputs());
        data.put("launchAllowed", loaderBoundary.launchAllowed());
        data.put("outputs", loaderBoundary.outputs());
        data.put("productionClassloaderAllowed", loaderBoundary.productionClassloaderAllowed());
        data.put("summary", loaderBoundary.summary());
        return data;
    }

    private static List<String> blockedPrototypeCapabilities() {
        return List.of(
                "game.launch",
                "production.classloader",
                "bytecode.transforms",
                "registry.injection",
                "native.library.extraction",
                "network.download",
                "installed.pack.mutation"
        );
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
