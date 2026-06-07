package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativeServiceBusPlan;
import dev.echo.nativeplatform.contracts.EchoNativeServiceBusRegistry;
import dev.echo.nativeplatform.contracts.EchoNativeServiceBusSafetyStatus;
import dev.echo.nativeplatform.contracts.EchoNativeServiceBusSimulationResult;
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

final class EchoNativeServiceBusPrototype {
    EchoNativeServiceBusPrototypeOutcome prototype(
            String packId,
            Path fixture,
            Path serviceGraphPath,
            Path lifecycleStubResultPath,
            Path lifecycleStubSafetyPath
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Object> serviceGraph = readRequiredReport(serviceGraphPath, fixture, packId, "ECHO-NATIVE-SERVICE-GRAPH-MISSING", "Service graph report missing", diagnostics);
        Map<String, Object> lifecycleStubResult = readRequiredReport(lifecycleStubResultPath, fixture, packId, "ECHO-NATIVE-LIFECYCLE-STUB-RESULT-MISSING", "Lifecycle stub execution result missing", diagnostics);
        Map<String, Object> lifecycleStubSafety = readRequiredReport(lifecycleStubSafetyPath, fixture, packId, "ECHO-NATIVE-LIFECYCLE-STUB-SAFETY-MISSING", "Lifecycle stub safety status missing", diagnostics);

        Map<String, Object> serviceGraphData = EchoNativeJson.asObject(serviceGraph.get("data"));
        Map<String, Object> lifecycleResultData = EchoNativeJson.asObject(lifecycleStubResult.get("data"));
        Map<String, Object> lifecycleSafetyData = EchoNativeJson.asObject(lifecycleStubSafety.get("data"));

        checkServiceGraph(serviceGraph, serviceGraphData, serviceGraphPath, packId, diagnostics);
        checkLifecycleStubResult(lifecycleStubResult, lifecycleResultData, lifecycleStubResultPath, packId, diagnostics);
        checkLifecycleStubSafety(lifecycleStubSafety, lifecycleSafetyData, lifecycleStubSafetyPath, packId, diagnostics);

        List<String> lifecycleModules = lifecycleModules(lifecycleResultData.get("lifecycleEvents"));
        List<Map<String, Object>> services = serviceItems(serviceGraphData.get("services"), lifecycleModules);
        Set<String> blockedServices = new LinkedHashSet<>();
        for (Map<String, Object> service : services) {
            String owner = String.valueOf(service.getOrDefault("providerModule", ""));
            if (owner.isBlank() || !lifecycleModules.contains(owner)) {
                String serviceId = String.valueOf(service.getOrDefault("id", "unknown.service"));
                blockedServices.add(serviceId);
                diagnostics.add(new EchoNativeDiagnostic(
                        "ECHO-NATIVE-SERVICE-BUS-OWNER-MISSING",
                        EchoNativeIssueSeverity.ERROR,
                        "Service bus owner was not present in lifecycle stubs",
                        "Service '" + serviceId + "' is owned by module '" + owner + "', which is not present in lifecycle-stub-execution-result.json.",
                        owner.isBlank() ? null : owner,
                        packId,
                        List.of(relativeReportPath(serviceGraphPath), relativeReportPath(lifecycleStubResultPath)),
                        "Regenerate service graph and lifecycle stubs from the same fixture snapshot."
                ));
            }
        }

        diagnostics = unique(diagnostics);
        boolean ready = diagnostics.isEmpty();
        List<Map<String, Object>> plannedServices = ready ? plannedServices(services, lifecycleModules) : List.of();
        List<Map<String, Object>> handles = ready ? serviceHandles(plannedServices) : List.of();
        List<String> registeredServices = handles.stream()
                .map(handle -> String.valueOf(handle.get("serviceId")))
                .toList();

        EchoNativeServiceBusPlan plan = new EchoNativeServiceBusPlan(
                "phase13.m10.service_bus.plan",
                ready,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                plannedServices.size(),
                plannedServices
        );
        EchoNativeServiceBusRegistry registry = new EchoNativeServiceBusRegistry(
                "phase13.m10.service_bus.registry",
                ready,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                handles.size(),
                handles
        );
        EchoNativeServiceBusSimulationResult result = new EchoNativeServiceBusSimulationResult(
                "phase13.m10.service_bus.simulation.result",
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
                false,
                false,
                registeredServices.size(),
                registeredServices,
                List.copyOf(blockedServices)
        );
        EchoNativeServiceBusSafetyStatus safety = new EchoNativeServiceBusSafetyStatus(
                "phase13.m10.service_bus.safety.status",
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
                false,
                false,
                ready ? List.of("service_graph_pass", "lifecycle_stubs_pass", "inert_handles_registered", "no_service_code_execution") : List.of()
        );

        return new EchoNativeServiceBusPrototypeOutcome(
                packId,
                serviceBusPlan(packId, plan, diagnostics),
                serviceBusRegistry(packId, registry, diagnostics),
                serviceBusSimulationResult(packId, result, diagnostics),
                serviceBusSafetyStatus(packId, safety, diagnostics),
                diagnostics
        );
    }

    private static void checkServiceGraph(
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
        List<Map<String, Object>> services = serviceItems(data.get("services"), List.of());
        if (!pass || services.isEmpty() || hasUnsafeRuntimeWork(data)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-SERVICE-GRAPH-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Service graph is not ready for the service bus prototype",
                    "M10 requires a PASS service-graph.json with declared descriptor services and no unsafe runtime work.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Regenerate native graph reports before prototyping the inert service bus."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static void checkLifecycleStubResult(
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
        boolean executed = Boolean.TRUE.equals(data.get("executed"));
        boolean stubOnly = Boolean.TRUE.equals(data.get("stubOnly"));
        boolean inertHandlers = Boolean.TRUE.equals(data.get("inertStubHandlersExecuted"));
        if (!pass || !executed || !stubOnly || !inertHandlers || hasUnsafeRuntimeWork(data)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-LIFECYCLE-STUBS-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Lifecycle stubs are not ready for the service bus prototype",
                    "M10 requires PASS lifecycle-stub-execution-result.json with executed=true, stubOnly=true, and inert handlers only.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Run echo-native phase13 execute lifecycle-stubs for a fixture whose M8 addon discovery passes."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static void checkLifecycleStubSafety(
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
        boolean safe = Boolean.TRUE.equals(data.get("safeToContinue"));
        boolean stubOnly = Boolean.TRUE.equals(data.get("stubOnly"));
        boolean inertHandlers = Boolean.TRUE.equals(data.get("inertHandlersOnly"));
        if (!pass || !safe || !stubOnly || !inertHandlers || hasUnsafeRuntimeWork(data)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-LIFECYCLE-STUB-SAFETY-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Lifecycle stub safety is not ready for the service bus prototype",
                    "M10 requires PASS lifecycle-stub-safety-status.json with safeToContinue=true and no unsafe runtime work.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Resolve lifecycle stub safety diagnostics before service bus prototyping."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static List<Map<String, Object>> plannedServices(List<Map<String, Object>> services, List<String> lifecycleModules) {
        List<Map<String, Object>> planned = new ArrayList<>();
        for (int index = 0; index < services.size(); index++) {
            Map<String, Object> service = services.get(index);
            String serviceId = String.valueOf(service.get("id"));
            String providerModule = String.valueOf(service.get("providerModule"));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("busScope", "IN_MEMORY_NATIVE_SERVICE_BUS");
            item.put("handleId", handleId(serviceId));
            item.put("inertHandle", true);
            item.put("lifecyclePhase", service.get("lifecyclePhase"));
            item.put("providedFeatures", stringList(service.get("providedFeatures")));
            item.put("providerModule", providerModule);
            item.put("providerModuleIndex", moduleIndex(lifecycleModules, providerModule));
            item.put("registrationOrder", index);
            item.put("serviceCodeExecuted", false);
            item.put("serviceId", serviceId);
            planned.add(item);
        }
        return List.copyOf(planned);
    }

    private static List<Map<String, Object>> serviceHandles(List<Map<String, Object>> plannedServices) {
        List<Map<String, Object>> handles = new ArrayList<>();
        for (Map<String, Object> plannedService : plannedServices) {
            Map<String, Object> handle = new LinkedHashMap<>();
            handle.put("handleId", plannedService.get("handleId"));
            handle.put("inert", true);
            handle.put("providerModule", plannedService.get("providerModule"));
            handle.put("registrationOrder", plannedService.get("registrationOrder"));
            handle.put("serviceCodeExecuted", false);
            handle.put("serviceId", plannedService.get("serviceId"));
            handle.put("state", "registered_in_memory");
            handles.add(handle);
        }
        return List.copyOf(handles);
    }

    private static List<Map<String, Object>> serviceItems(Object value, List<String> lifecycleModules) {
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
                .comparingInt((Map<String, Object> service) -> moduleIndex(lifecycleModules, String.valueOf(service.get("providerModule"))))
                .thenComparing(service -> String.valueOf(service.get("id"))));
        return List.copyOf(services);
    }

    private static List<String> lifecycleModules(Object eventsValue) {
        if (!(eventsValue instanceof List<?> events)) {
            return List.of();
        }
        Set<String> modules = new LinkedHashSet<>();
        for (Object eventValue : events) {
            Map<String, Object> event = EchoNativeJson.asObject(eventValue);
            String moduleId = String.valueOf(event.getOrDefault("moduleId", ""));
            if (!moduleId.isBlank()) {
                modules.add(moduleId);
            }
        }
        return List.copyOf(modules);
    }

    private static Map<String, Object> serviceBusPlan(
            String packId,
            EchoNativeServiceBusPlan plan,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m10_service_bus_plan", diagnostics);
        data.put("classloaderCreated", plan.classloaderCreated());
        data.put("gameClassesResolved", plan.gameClassesResolved());
        data.put("inMemoryOnly", plan.inMemoryOnly());
        data.put("inertHandlesOnly", plan.inertHandlesOnly());
        data.put("packId", packId);
        data.put("planId", plan.planId());
        data.put("plannedServiceCount", plan.plannedServiceCount());
        data.put("plannedServices", plan.plannedServices());
        data.put("processLaunched", plan.processLaunched());
        data.put("ready", plan.ready());
        data.put("registryMutated", plan.registryMutated());
        data.put("serviceCodeExecuted", plan.serviceCodeExecuted());
        data.put("summary", plan.ready()
                ? "In-memory service bus plan is ready with descriptor-declared inert service handles."
                : "In-memory service bus plan is blocked by upstream diagnostics.");
        return data;
    }

    private static Map<String, Object> serviceBusRegistry(
            String packId,
            EchoNativeServiceBusRegistry registry,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m10_service_bus_registry", diagnostics);
        data.put("classloaderCreated", registry.classloaderCreated());
        data.put("gameClassesResolved", registry.gameClassesResolved());
        data.put("inMemoryOnly", registry.inMemoryOnly());
        data.put("inertHandlesOnly", registry.inertHandlesOnly());
        data.put("packId", packId);
        data.put("processLaunched", registry.processLaunched());
        data.put("registered", registry.registered());
        data.put("registryId", registry.registryId());
        data.put("registryMutated", registry.registryMutated());
        data.put("serviceCodeExecuted", registry.serviceCodeExecuted());
        data.put("serviceHandleCount", registry.serviceHandleCount());
        data.put("serviceHandles", registry.serviceHandles());
        data.put("summary", registry.registered()
                ? "Service handles were registered in an in-memory native service bus only."
                : "Service bus registry was not built because upstream diagnostics blocked M10.");
        return data;
    }

    private static Map<String, Object> serviceBusSimulationResult(
            String packId,
            EchoNativeServiceBusSimulationResult result,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m10_service_bus_simulation_result", diagnostics);
        data.put("addonCodeExecuted", result.addonCodeExecuted());
        data.put("blockedServiceCount", result.blockedServices().size());
        data.put("blockedServices", result.blockedServices());
        data.put("classloaderCreated", result.classloaderCreated());
        data.put("commandExecuted", result.commandExecuted());
        data.put("filesystemMutated", result.filesystemMutated());
        data.put("gameClassesResolved", result.gameClassesResolved());
        data.put("inMemoryOnly", result.inMemoryOnly());
        data.put("inertHandlesOnly", result.inertHandlesOnly());
        data.put("packId", packId);
        data.put("processLaunched", result.processLaunched());
        data.put("registeredServiceCount", result.registeredServiceCount());
        data.put("registeredServices", result.registeredServices());
        data.put("registryInjected", result.registryInjected());
        data.put("registryMutated", result.registryMutated());
        data.put("resultId", result.resultId());
        data.put("serviceCodeExecuted", result.serviceCodeExecuted());
        data.put("simulated", result.simulated());
        data.put("summary", result.simulated()
                ? "Service bus prototype registered descriptor-declared services as inert in-memory handles only."
                : "Service bus prototype did not run because required M9 reports were missing, failed, or unsafe.");
        return data;
    }

    private static Map<String, Object> serviceBusSafetyStatus(
            String packId,
            EchoNativeServiceBusSafetyStatus status,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m10_service_bus_safety_status", diagnostics);
        data.put("addonCodeExecuted", status.addonCodeExecuted());
        data.put("classloaderCreated", status.classloaderCreated());
        data.put("commandExecuted", status.commandExecuted());
        data.put("completedChecks", status.completedChecks());
        data.put("filesystemMutated", status.filesystemMutated());
        data.put("gameClassesResolved", status.gameClassesResolved());
        data.put("inMemoryOnly", status.inMemoryOnly());
        data.put("inertHandlesOnly", status.inertHandlesOnly());
        data.put("packId", packId);
        data.put("processLaunched", status.processLaunched());
        data.put("registryInjected", status.registryInjected());
        data.put("registryMutated", status.registryMutated());
        data.put("safeToContinue", status.safeToContinue());
        data.put("serviceCodeExecuted", status.serviceCodeExecuted());
        data.put("statusId", status.statusId());
        data.put("summary", status.safeToContinue()
                ? "M10 service bus stayed in-memory, inert, and safe to continue."
                : "M10 service bus is blocked by diagnostics.");
        return data;
    }

    private static Map<String, Object> base(String phase, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("addonCodeExecuted", false);
        data.put("commandExecuted", false);
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
        data.put("serviceCodeExecuted", false);
        data.put("simulationOnly", true);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
    }

    private static boolean hasUnsafeRuntimeWork(Map<String, Object> data) {
        return Boolean.TRUE.equals(data.get("addonCodeExecuted"))
                || Boolean.TRUE.equals(data.get("realAddonCodeExecuted"))
                || Boolean.TRUE.equals(data.get("serviceCodeExecuted"))
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
                    "Required M10 service bus input report was not found.",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate service graph and M9 lifecycle stub reports before service bus prototyping."
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
        result.sort(String::compareTo);
        return List.copyOf(result);
    }

    private static int moduleIndex(List<String> modules, String moduleId) {
        int index = modules.indexOf(moduleId);
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    private static String handleId(String serviceId) {
        return "inert://" + serviceId.replace(':', '/');
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
