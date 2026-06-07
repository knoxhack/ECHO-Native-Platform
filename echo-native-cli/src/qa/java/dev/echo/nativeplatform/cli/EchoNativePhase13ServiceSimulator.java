package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeCrashBoundaryVerification;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativeServiceAttachSimulationResult;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class EchoNativePhase13ServiceSimulator {
    EchoNativePhase13ServiceSimulationOutcome simulate(
            String packId,
            Path fixture,
            Path phase13PlanPath,
            Path serviceGraphPath,
            Path lifecycleResultPath
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Object> phase13Plan = readRequiredReport(phase13PlanPath, fixture, packId, "ECHO-NATIVE-PHASE13-PLAN-MISSING", "Phase 13 plan report missing", diagnostics);
        Map<String, Object> serviceGraph = readRequiredReport(serviceGraphPath, fixture, packId, "ECHO-NATIVE-SERVICE-GRAPH-MISSING", "Service graph report missing", diagnostics);
        Map<String, Object> lifecycleResult = readRequiredReport(lifecycleResultPath, fixture, packId, "ECHO-NATIVE-LIFECYCLE-RESULT-MISSING", "Lifecycle simulation result missing", diagnostics);

        Map<String, Object> phase13Data = EchoNativeJson.asObject(phase13Plan.get("data"));
        Map<String, Object> serviceData = EchoNativeJson.asObject(serviceGraph.get("data"));
        Map<String, Object> lifecycleData = EchoNativeJson.asObject(lifecycleResult.get("data"));

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
                        "Phase 13 plan not ready for service simulation",
                        "Service attach simulation requires a PASS phase13-plan.json with planning enabled and no runtime work started.",
                        null,
                        packId,
                        List.of(relativeReportPath(phase13PlanPath)),
                        "Run echo-native phase13 plan for a fixture with PASS Phase 13 readiness first."
                ));
            }
            diagnostics.addAll(reportDiagnostics(phase13Plan, packId));
        }

        if (!serviceGraph.isEmpty() && !"PASS".equals(serviceGraph.get("status"))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-SERVICE-GRAPH-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Service graph is not PASS",
                    "Service attach simulation requires a PASS service-graph.json.",
                    null,
                    packId,
                    List.of(relativeReportPath(serviceGraphPath)),
                    "Resolve graph diagnostics before simulating service attachment."
            ));
            diagnostics.addAll(reportDiagnostics(serviceGraph, packId));
        }

        if (!lifecycleResult.isEmpty()) {
            boolean pass = "PASS".equals(lifecycleResult.get("status"));
            boolean simulated = Boolean.TRUE.equals(lifecycleData.get("simulated"));
            boolean simulationOnly = Boolean.TRUE.equals(lifecycleData.get("simulationOnly"));
            boolean classloaderCreated = Boolean.TRUE.equals(lifecycleData.get("classloaderCreated"));
            boolean processLaunched = Boolean.TRUE.equals(lifecycleData.get("processLaunched"));
            if (!pass || !simulated || !simulationOnly || classloaderCreated || processLaunched) {
                diagnostics.add(new EchoNativeDiagnostic(
                        "ECHO-NATIVE-LIFECYCLE-SIMULATION-BLOCKED",
                        EchoNativeIssueSeverity.ERROR,
                        "Lifecycle simulation result is not ready",
                        "Service attach simulation requires a PASS lifecycle-simulation-result.json that is simulation-only and created no classloader or process.",
                        null,
                        packId,
                        List.of(relativeReportPath(lifecycleResultPath)),
                        "Run echo-native phase13 simulate lifecycle for a fixture whose Phase 13 plan passes."
                ));
            }
            diagnostics.addAll(reportDiagnostics(lifecycleResult, packId));
        }

        List<String> simulatedModules = stringList(lifecycleData.get("simulatedModules"));
        List<Map<String, Object>> services = serviceItems(serviceData.get("services"), simulatedModules);
        Set<String> blockedServices = new LinkedHashSet<>();
        for (Map<String, Object> service : services) {
            String owner = String.valueOf(service.getOrDefault("providerModule", ""));
            if (owner.isBlank() || !simulatedModules.contains(owner)) {
                String serviceId = String.valueOf(service.getOrDefault("id", "unknown.service"));
                blockedServices.add(serviceId);
                diagnostics.add(new EchoNativeDiagnostic(
                        "ECHO-NATIVE-SERVICE-OWNER-NOT-SIMULATED",
                        EchoNativeIssueSeverity.ERROR,
                        "Service owner was not simulated",
                        "Service '" + serviceId + "' is owned by module '" + owner + "', which is not present in lifecycle-simulation-result.json.",
                        owner.isBlank() ? null : owner,
                        packId,
                        List.of(relativeReportPath(serviceGraphPath), relativeReportPath(lifecycleResultPath)),
                        "Regenerate graph and lifecycle simulation reports from the same fixture snapshot."
                ));
            }
        }

        diagnostics = unique(diagnostics);
        boolean canSimulate = diagnostics.isEmpty();
        List<Map<String, Object>> attachOrder = canSimulate ? attachOrder(services, simulatedModules) : List.of();
        List<String> attachedServices = attachOrder.stream()
                .map(item -> String.valueOf(item.get("id")))
                .toList();
        List<String> attachedModules = attachOrder.stream()
                .map(item -> String.valueOf(item.get("providerModule")))
                .distinct()
                .toList();
        EchoNativeServiceAttachSimulationResult serviceResult = new EchoNativeServiceAttachSimulationResult(
                "phase13.service.attach.simulation.result",
                canSimulate,
                false,
                false,
                false,
                attachedServices,
                attachedModules,
                List.copyOf(blockedServices)
        );
        EchoNativeCrashBoundaryVerification crashVerification = new EchoNativeCrashBoundaryVerification(
                "phase13.crash.boundary.verification",
                canSimulate,
                true,
                true,
                false,
                false,
                canSimulate ? 0 : diagnostics.size(),
                List.of("phase13-plan.json", "service-graph.json", "lifecycle-simulation-result.json", "service-attach-simulation-result.json")
        );

        return new EchoNativePhase13ServiceSimulationOutcome(
                packId,
                serviceAttachSimulationResult(packId, serviceResult, diagnostics, services, attachOrder),
                crashBoundaryVerification(packId, crashVerification, diagnostics),
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
                    "Required Phase 13 service simulation input report was not found.",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate Phase 13 planning, graph, and lifecycle simulation reports before service simulation."
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

    private static List<Map<String, Object>> serviceItems(Object value, List<String> simulatedModules) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> services = new ArrayList<>();
        for (Object item : list) {
            Map<String, Object> service = EchoNativeJson.asObject(item);
            if (!service.isEmpty()) {
                services.add(service);
            }
        }
        services.sort(Comparator
                .comparingInt((Map<String, Object> service) -> moduleIndex(simulatedModules, String.valueOf(service.get("providerModule"))))
                .thenComparing(service -> String.valueOf(service.get("id"))));
        return List.copyOf(services);
    }

    private static List<Map<String, Object>> attachOrder(List<Map<String, Object>> services, List<String> simulatedModules) {
        List<Map<String, Object>> attached = new ArrayList<>();
        for (int index = 0; index < services.size(); index++) {
            Map<String, Object> service = services.get(index);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("attachIndex", index);
            item.put("id", service.get("id"));
            item.put("lifecyclePhase", service.get("lifecyclePhase"));
            item.put("providedFeatures", stringList(service.get("providedFeatures")));
            item.put("providerModule", service.get("providerModule"));
            item.put("providerModuleIndex", moduleIndex(simulatedModules, String.valueOf(service.get("providerModule"))));
            item.put("simulationBoundary", "IN_MEMORY_SERVICE_ATTACH");
            attached.add(item);
        }
        return List.copyOf(attached);
    }

    private static Map<String, Object> serviceAttachSimulationResult(
            String packId,
            EchoNativeServiceAttachSimulationResult result,
            List<EchoNativeDiagnostic> diagnostics,
            List<Map<String, Object>> plannedServices,
            List<Map<String, Object>> attachOrder
    ) {
        Map<String, Object> data = base("phase13_service_attach_simulation_result", diagnostics);
        data.put("attachedModules", result.attachedModules());
        data.put("attachedServiceCount", result.attachedServices().size());
        data.put("attachedServices", result.attachedServices());
        data.put("blockedServiceCount", result.blockedServices().size());
        data.put("blockedServices", result.blockedServices());
        data.put("classloaderCreated", result.classloaderCreated());
        data.put("executedServiceCode", result.executedServiceCode());
        data.put("packId", packId);
        data.put("plannedServiceCount", plannedServices.size());
        data.put("processLaunched", result.processLaunched());
        data.put("serviceAttachOrder", attachOrder);
        data.put("serviceAttachSimulated", result.simulated());
        data.put("simulationId", result.simulationId());
        data.put("summary", result.simulated()
                ? "Service attachment order was simulated in memory from service-graph.json and lifecycle-simulation-result.json."
                : "Service attachment simulation was blocked by upstream Phase 13 diagnostics.");
        return data;
    }

    private static Map<String, Object> crashBoundaryVerification(
            String packId,
            EchoNativeCrashBoundaryVerification result,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_crash_boundary_verification", diagnostics);
        data.put("capturedDiagnostics", result.capturedDiagnostics());
        data.put("crashBoundaryActive", result.crashBoundaryActive());
        data.put("mutatedState", result.mutatedState());
        data.put("packId", packId);
        data.put("simulatedFailureCount", result.simulatedFailureCount());
        data.put("terminatedProcess", result.terminatedProcess());
        data.put("verificationId", result.verificationId());
        data.put("verified", result.verified());
        data.put("verifiedBoundaries", result.verifiedBoundaries());
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

    private static int moduleIndex(List<String> modules, String moduleId) {
        int index = modules.indexOf(moduleId);
        return index < 0 ? Integer.MAX_VALUE : index;
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
