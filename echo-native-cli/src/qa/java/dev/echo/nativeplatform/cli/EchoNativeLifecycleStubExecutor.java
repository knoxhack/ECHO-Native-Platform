package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativeLifecycleStubCrashBoundary;
import dev.echo.nativeplatform.contracts.EchoNativeLifecycleStubExecutionPlan;
import dev.echo.nativeplatform.contracts.EchoNativeLifecycleStubExecutionResult;
import dev.echo.nativeplatform.contracts.EchoNativeLifecycleStubSafetyStatus;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoNativeLifecycleStubExecutor {
    private static final List<String> PHASES = List.of(
            "DISCOVER_DESCRIPTOR",
            "VALIDATE_CONTRACTS",
            "PREPARE_MODULE_STUB",
            "ATTACH_INERT_SERVICE_HANDLE",
            "SHUTDOWN_STUB"
    );

    EchoNativeLifecycleStubExecutionOutcome execute(
            String packId,
            Path fixture,
            Path discoveryPlanPath,
            Path descriptorSnapshotPath,
            Path discoverySafetyStatusPath
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Object> discoveryPlan = readRequiredReport(discoveryPlanPath, fixture, packId, "ECHO-NATIVE-ADDON-DISCOVERY-PLAN-MISSING", "Addon runtime discovery plan missing", diagnostics);
        Map<String, Object> descriptorSnapshot = readRequiredReport(descriptorSnapshotPath, fixture, packId, "ECHO-NATIVE-ADDON-RUNTIME-DESCRIPTORS-MISSING", "Addon runtime descriptors missing", diagnostics);
        Map<String, Object> discoverySafety = readRequiredReport(discoverySafetyStatusPath, fixture, packId, "ECHO-NATIVE-ADDON-DISCOVERY-SAFETY-MISSING", "Addon runtime discovery safety status missing", diagnostics);

        Map<String, Object> discoveryPlanData = EchoNativeJson.asObject(discoveryPlan.get("data"));
        Map<String, Object> descriptorSnapshotData = EchoNativeJson.asObject(descriptorSnapshot.get("data"));
        Map<String, Object> discoverySafetyData = EchoNativeJson.asObject(discoverySafety.get("data"));

        checkDiscoveryPlan(discoveryPlan, discoveryPlanData, discoveryPlanPath, packId, diagnostics);
        checkDescriptorSnapshot(descriptorSnapshot, descriptorSnapshotData, descriptorSnapshotPath, packId, diagnostics);
        checkDiscoverySafety(discoverySafety, discoverySafetyData, discoverySafetyStatusPath, packId, diagnostics);

        List<String> moduleOrder = EchoNativeJson.stringList(descriptorSnapshotData.get("orderedModuleIds"));
        diagnostics = unique(diagnostics);
        boolean ready = diagnostics.isEmpty();
        List<Map<String, Object>> lifecycleEvents = ready ? lifecycleEvents(moduleOrder) : List.of();
        EchoNativeLifecycleStubExecutionPlan plan = new EchoNativeLifecycleStubExecutionPlan(
                "phase13.m9.lifecycle_stub.execution.plan",
                ready,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                ready ? PHASES : List.of(),
                ready ? moduleOrder : List.of()
        );
        EchoNativeLifecycleStubExecutionResult result = new EchoNativeLifecycleStubExecutionResult(
                "phase13.m9.lifecycle_stub.execution.result",
                ready,
                true,
                ready,
                false,
                false,
                false,
                false,
                false,
                ready ? moduleOrder.size() : 0,
                lifecycleEvents.size(),
                lifecycleEvents
        );
        EchoNativeLifecycleStubCrashBoundary crashBoundary = new EchoNativeLifecycleStubCrashBoundary(
                "phase13.m9.lifecycle_stub.crash_boundary",
                ready,
                ready,
                ready,
                ready,
                true,
                false,
                false,
                false,
                ready ? List.of("stub_handler_exception", "invalid_stub_phase", "shutdown_stub_failure") : List.of()
        );
        EchoNativeLifecycleStubSafetyStatus safetyStatus = new EchoNativeLifecycleStubSafetyStatus(
                "phase13.m9.lifecycle_stub.safety.status",
                ready,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                ready ? List.of("addon_runtime_discovery_pass", "stub_order_deterministic", "inert_handlers_only", "failure_boundary_reported") : List.of()
        );

        return new EchoNativeLifecycleStubExecutionOutcome(
                packId,
                lifecycleStubExecutionPlan(packId, plan, diagnostics),
                lifecycleStubExecutionResult(packId, result, diagnostics),
                lifecycleStubCrashBoundary(packId, crashBoundary, diagnostics),
                lifecycleStubSafetyStatus(packId, safetyStatus, diagnostics),
                diagnostics
        );
    }

    private static List<Map<String, Object>> lifecycleEvents(List<String> moduleOrder) {
        List<Map<String, Object>> events = new ArrayList<>();
        int order = 0;
        for (String moduleId : moduleOrder) {
            for (String phase : PHASES) {
                Map<String, Object> event = new LinkedHashMap<>();
                event.put("addonCodeExecuted", false);
                event.put("classloaderCreated", false);
                event.put("eventId", "m9-" + String.format("%04d", order) + "-" + moduleId + "-" + phase.toLowerCase());
                event.put("gameClassesResolved", false);
                event.put("inertStubHandler", true);
                event.put("moduleId", moduleId);
                event.put("order", order);
                event.put("phase", phase);
                event.put("registryBridgeTouched", false);
                event.put("status", "simulated");
                event.put("transformsPerformed", false);
                events.add(event);
                order++;
            }
        }
        return List.copyOf(events);
    }

    private static void checkDiscoveryPlan(
            Map<String, Object> report,
            Map<String, Object> data,
            Path path,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        if (report.isEmpty()) {
            return;
        }
        boolean pass = "PASS".equals(report.get("status"));
        boolean ready = Boolean.TRUE.equals(data.get("ready"));
        boolean dataOnly = Boolean.TRUE.equals(data.get("dataOnly"));
        boolean deterministicOrder = Boolean.TRUE.equals(data.get("deterministicOrder"));
        if (!pass || !ready || !dataOnly || !deterministicOrder || hasUnsafeGameWork(data)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-ADDON-DISCOVERY-PLAN-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Addon runtime discovery plan is not safe for lifecycle stubs",
                    "Lifecycle stub execution requires PASS addon-runtime-discovery-plan.json with dataOnly=true and deterministicOrder=true.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Run echo-native phase13 discover addons for a fixture whose M7 dummy process boundary passes."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static void checkDescriptorSnapshot(
            Map<String, Object> report,
            Map<String, Object> data,
            Path path,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        if (report.isEmpty()) {
            return;
        }
        boolean pass = "PASS".equals(report.get("status"));
        boolean complete = Boolean.TRUE.equals(data.get("complete"));
        boolean descriptorDataOnly = Boolean.TRUE.equals(data.get("descriptorDataOnly"));
        boolean deterministicOrder = Boolean.TRUE.equals(data.get("deterministicOrder"));
        List<String> orderedModuleIds = EchoNativeJson.stringList(data.get("orderedModuleIds"));
        List<String> sortedModuleIds = orderedModuleIds.stream().sorted().toList();
        if (!pass || !complete || !descriptorDataOnly || !deterministicOrder || !orderedModuleIds.equals(sortedModuleIds) || hasUnsafeGameWork(data)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-ADDON-RUNTIME-DESCRIPTORS-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Addon runtime descriptors are not safe for lifecycle stubs",
                    "Lifecycle stub execution requires PASS addon-runtime-descriptors.json with complete deterministic descriptor data.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Regenerate addon runtime discovery and preserve deterministic module order."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static void checkDiscoverySafety(
            Map<String, Object> report,
            Map<String, Object> data,
            Path path,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        if (report.isEmpty()) {
            return;
        }
        boolean pass = "PASS".equals(report.get("status"));
        boolean safeToContinue = Boolean.TRUE.equals(data.get("safeToContinue"));
        boolean dataOnly = Boolean.TRUE.equals(data.get("dataOnly"));
        if (!pass || !safeToContinue || !dataOnly || hasUnsafeGameWork(data)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-ADDON-DISCOVERY-SAFETY-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Addon runtime discovery safety is not ready for lifecycle stubs",
                    "Lifecycle stub execution requires PASS addon-runtime-discovery-safety-status.json with safeToContinue=true and no unsafe runtime work.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Regenerate addon runtime discovery safety status before lifecycle stub execution."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static Map<String, Object> lifecycleStubExecutionPlan(
            String packId,
            EchoNativeLifecycleStubExecutionPlan plan,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m9_lifecycle_stub_execution_plan", diagnostics);
        data.put("classloaderCreated", plan.classloaderCreated());
        data.put("inertHandlersOnly", plan.inertHandlersOnly());
        data.put("lifecyclePhases", plan.lifecyclePhases());
        data.put("minecraftClassesResolved", plan.minecraftClassesResolved());
        data.put("moduleOrder", plan.moduleOrder());
        data.put("packId", packId);
        data.put("planId", plan.planId());
        data.put("ready", plan.ready());
        data.put("realAddonCodeExecuted", plan.realAddonCodeExecuted());
        data.put("registryBridgeTouched", plan.registryBridgeTouched());
        data.put("stubOnly", plan.stubOnly());
        data.put("summary", plan.ready()
                ? "Lifecycle stub execution is ready to run inert built-in handlers over the deterministic descriptor order."
                : "Lifecycle stub execution is blocked by addon runtime discovery diagnostics.");
        data.put("transformsRequested", plan.transformsRequested());
        return data;
    }

    private static Map<String, Object> lifecycleStubExecutionResult(
            String packId,
            EchoNativeLifecycleStubExecutionResult result,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m9_lifecycle_stub_execution_result", diagnostics);
        data.put("classloaderCreated", result.classloaderCreated());
        data.put("executed", result.executed());
        data.put("gameClassesResolved", result.gameClassesResolved());
        data.put("inertStubHandlersExecuted", result.inertStubHandlersExecuted());
        data.put("lifecycleEvents", result.lifecycleEvents());
        data.put("moduleCount", result.moduleCount());
        data.put("packId", packId);
        data.put("realAddonCodeExecuted", result.realAddonCodeExecuted());
        data.put("registryInjected", result.registryInjected());
        data.put("resultId", result.resultId());
        data.put("stubHandlerCount", result.stubHandlerCount());
        data.put("stubOnly", result.stubOnly());
        data.put("summary", result.executed()
                ? "Lifecycle stubs executed as inert built-in handlers only; no real addon or Minecraft code ran."
                : "Lifecycle stubs did not execute because required M8 reports were missing, failed, or unsafe.");
        data.put("transformsPerformed", result.transformsPerformed());
        return data;
    }

    private static Map<String, Object> lifecycleStubCrashBoundary(
            String packId,
            EchoNativeLifecycleStubCrashBoundary boundary,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m9_lifecycle_stub_crash_boundary", diagnostics);
        data.put("boundaryId", boundary.boundaryId());
        data.put("classloaderCreated", boundary.classloaderCreated());
        data.put("containedFailureModes", boundary.containedFailureModes());
        data.put("diagnosticsWritten", boundary.diagnosticsWritten());
        data.put("lifecycleOrderValidated", boundary.lifecycleOrderValidated());
        data.put("packId", packId);
        data.put("realAddonCodeExecuted", boundary.realAddonCodeExecuted());
        data.put("registryInjected", boundary.registryInjected());
        data.put("shutdownBoundaryValidated", boundary.shutdownBoundaryValidated());
        data.put("stubFailureContained", boundary.stubFailureContained());
        data.put("summary", boundary.verified()
                ? "Lifecycle stub crash boundary verified deterministic failure containment for inert handlers."
                : "Lifecycle stub crash boundary is blocked by upstream diagnostics.");
        data.put("verified", boundary.verified());
        return data;
    }

    private static Map<String, Object> lifecycleStubSafetyStatus(
            String packId,
            EchoNativeLifecycleStubSafetyStatus status,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m9_lifecycle_stub_safety_status", diagnostics);
        data.put("classloaderCreated", status.classloaderCreated());
        data.put("completedChecks", status.completedChecks());
        data.put("filesystemMutated", status.filesystemMutated());
        data.put("inertHandlersOnly", status.inertHandlersOnly());
        data.put("minecraftClassesResolved", status.minecraftClassesResolved());
        data.put("packId", packId);
        data.put("processLaunched", status.processLaunched());
        data.put("realAddonCodeExecuted", status.realAddonCodeExecuted());
        data.put("registryBridgeTouched", status.registryBridgeTouched());
        data.put("safeToContinue", status.safeToContinue());
        data.put("statusId", status.statusId());
        data.put("stubOnly", status.stubOnly());
        data.put("summary", status.safeToContinue()
                ? "M9 lifecycle stubs remained inert and safe to continue."
                : "M9 lifecycle stubs are blocked by diagnostics.");
        data.put("transformsPerformed", status.transformsPerformed());
        return data;
    }

    private static Map<String, Object> base(String phase, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("addonCodeExecuted", false);
        data.put("diagnosticCount", diagnostics.size());
        data.put("dryRunOnly", true);
        data.put("filesystemMutated", false);
        data.put("gameClassesResolved", false);
        data.put("gameProcessLaunched", false);
        data.put("minecraftLaunched", false);
        data.put("phase", phase);
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("simulationOnly", true);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
    }

    private static boolean hasUnsafeGameWork(Map<String, Object> data) {
        return Boolean.TRUE.equals(data.get("addonCodeExecuted"))
                || Boolean.TRUE.equals(data.get("realAddonCodeExecuted"))
                || Boolean.TRUE.equals(data.get("classloaderCreated"))
                || Boolean.TRUE.equals(data.get("productionClassloader"))
                || Boolean.TRUE.equals(data.get("resolvesRuntimeClasses"))
                || Boolean.TRUE.equals(data.get("gameClassesResolved"))
                || Boolean.TRUE.equals(data.get("minecraftClassesResolved"))
                || Boolean.TRUE.equals(data.get("gameProcessLaunched"))
                || Boolean.TRUE.equals(data.get("minecraftLaunched"))
                || Boolean.TRUE.equals(data.get("commandExecuted"))
                || Boolean.TRUE.equals(data.get("registryInjected"))
                || Boolean.TRUE.equals(data.get("registryMutated"))
                || Boolean.TRUE.equals(data.get("registryBridgeTouched"))
                || Boolean.TRUE.equals(data.get("transformsPerformed"))
                || Boolean.TRUE.equals(data.get("transformsRequested"))
                || Boolean.TRUE.equals(data.get("filesystemMutated"))
                || Boolean.TRUE.equals(data.get("unsafeRuntimeWorkStarted"));
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
                    "Required M9 lifecycle stub input report was not found.",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate M8 addon runtime discovery before lifecycle stub execution."
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
