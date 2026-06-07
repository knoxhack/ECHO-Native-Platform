package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeBoundaryFailureCase;
import dev.echo.nativeplatform.contracts.EchoNativeClassloaderBoundaryRehearsal;
import dev.echo.nativeplatform.contracts.EchoNativeCrashBoundarySimulationResult;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoNativePhase13CrashBoundarySimulator {
    EchoNativePhase13CrashBoundarySimulationOutcome simulate(
            String packId,
            Path fixture,
            Path phase13PlanPath,
            Path lifecycleResultPath,
            Path serviceAttachResultPath,
            Path classloaderBoundaryPlanPath
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Object> phase13Plan = readRequiredReport(phase13PlanPath, fixture, packId, "ECHO-NATIVE-PHASE13-PLAN-MISSING", "Phase 13 plan report missing", diagnostics);
        Map<String, Object> lifecycleResult = readRequiredReport(lifecycleResultPath, fixture, packId, "ECHO-NATIVE-LIFECYCLE-RESULT-MISSING", "Lifecycle simulation result missing", diagnostics);
        Map<String, Object> serviceAttachResult = readRequiredReport(serviceAttachResultPath, fixture, packId, "ECHO-NATIVE-SERVICE-ATTACH-RESULT-MISSING", "Service attach simulation result missing", diagnostics);
        Map<String, Object> classloaderBoundaryPlan = readRequiredReport(classloaderBoundaryPlanPath, fixture, packId, "ECHO-NATIVE-CLASSLOADER-BOUNDARY-PLAN-MISSING", "Classloader boundary plan missing", diagnostics);

        Map<String, Object> phase13Data = EchoNativeJson.asObject(phase13Plan.get("data"));
        Map<String, Object> lifecycleData = EchoNativeJson.asObject(lifecycleResult.get("data"));
        Map<String, Object> serviceData = EchoNativeJson.asObject(serviceAttachResult.get("data"));
        Map<String, Object> classloaderData = EchoNativeJson.asObject(classloaderBoundaryPlan.get("data"));

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
                        "Phase 13 plan not ready for crash-boundary simulation",
                        "Crash-boundary simulation requires a PASS phase13-plan.json with planning enabled and no runtime work started.",
                        null,
                        packId,
                        List.of(relativeReportPath(phase13PlanPath)),
                        "Run echo-native phase13 plan for a fixture with PASS Phase 13 readiness first."
                ));
            }
            diagnostics.addAll(reportDiagnostics(phase13Plan, packId));
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
                        "Crash-boundary simulation requires a PASS lifecycle-simulation-result.json that is simulation-only and created no classloader or process.",
                        null,
                        packId,
                        List.of(relativeReportPath(lifecycleResultPath)),
                        "Run echo-native phase13 simulate lifecycle for a fixture whose Phase 13 plan passes."
                ));
            }
            diagnostics.addAll(reportDiagnostics(lifecycleResult, packId));
        }

        if (!serviceAttachResult.isEmpty()) {
            boolean pass = "PASS".equals(serviceAttachResult.get("status"));
            boolean simulated = Boolean.TRUE.equals(serviceData.get("serviceAttachSimulated"));
            boolean simulationOnly = Boolean.TRUE.equals(serviceData.get("simulationOnly"));
            boolean classloaderCreated = Boolean.TRUE.equals(serviceData.get("classloaderCreated"));
            boolean processLaunched = Boolean.TRUE.equals(serviceData.get("processLaunched"));
            boolean executedServiceCode = Boolean.TRUE.equals(serviceData.get("executedServiceCode"));
            if (!pass || !simulated || !simulationOnly || classloaderCreated || processLaunched || executedServiceCode) {
                diagnostics.add(new EchoNativeDiagnostic(
                        "ECHO-NATIVE-SERVICE-SIMULATION-BLOCKED",
                        EchoNativeIssueSeverity.ERROR,
                        "Service attach simulation result is not ready",
                        "Crash-boundary simulation requires a PASS service-attach-simulation-result.json with no service code execution, classloader, or process.",
                        null,
                        packId,
                        List.of(relativeReportPath(serviceAttachResultPath)),
                        "Run echo-native phase13 simulate services for a fixture whose service graph and lifecycle simulation pass."
                ));
            }
            diagnostics.addAll(reportDiagnostics(serviceAttachResult, packId));
        }

        if (!classloaderBoundaryPlan.isEmpty()) {
            boolean pass = "PASS".equals(classloaderBoundaryPlan.get("status"));
            boolean classloaderCreated = Boolean.TRUE.equals(classloaderData.get("classloaderCreated"));
            boolean productionClassloader = Boolean.TRUE.equals(classloaderData.get("productionClassloader"));
            boolean resolvesRuntimeClasses = Boolean.TRUE.equals(classloaderData.get("resolvesRuntimeClasses"));
            if (!pass || classloaderCreated || productionClassloader || resolvesRuntimeClasses) {
                diagnostics.add(new EchoNativeDiagnostic(
                        "ECHO-NATIVE-CLASSLOADER-BOUNDARY-BLOCKED",
                        EchoNativeIssueSeverity.ERROR,
                        "Classloader boundary plan is not safe for rehearsal",
                        "Classloader boundary rehearsal requires a PASS classloader-boundary-plan.json with classloaderCreated=false and resolvesRuntimeClasses=false.",
                        null,
                        packId,
                        List.of(relativeReportPath(classloaderBoundaryPlanPath)),
                        "Regenerate Phase 13 planning reports and keep classloader work data-only."
                ));
            }
            diagnostics.addAll(reportDiagnostics(classloaderBoundaryPlan, packId));
        }

        diagnostics = unique(diagnostics);
        boolean canSimulate = diagnostics.isEmpty();
        List<EchoNativeBoundaryFailureCase> cases = failureCases();
        EchoNativeCrashBoundarySimulationResult crashResult = new EchoNativeCrashBoundarySimulationResult(
                "phase13.crash.boundary.simulation.result",
                canSimulate,
                true,
                true,
                false,
                false,
                false,
                false,
                canSimulate ? cases.stream().map(EchoNativeBoundaryFailureCase::id).toList() : List.of()
        );
        EchoNativeClassloaderBoundaryRehearsal rehearsal = new EchoNativeClassloaderBoundaryRehearsal(
                "phase13.classloader.boundary.rehearsal",
                canSimulate,
                false,
                false,
                false,
                false,
                canSimulate ? stringList(classloaderData.get("plannedBoundaries")) : List.of(),
                stringList(classloaderData.get("blockedCapabilities"))
        );

        return new EchoNativePhase13CrashBoundarySimulationOutcome(
                packId,
                crashBoundarySimulationResult(packId, crashResult, diagnostics, cases),
                boundaryFailureCases(packId, cases, diagnostics, canSimulate),
                classloaderBoundaryRehearsal(packId, rehearsal, diagnostics),
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
                    "Required Phase 13 crash-boundary simulation input report was not found.",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate Phase 13 planning, lifecycle, and service simulation reports before crash-boundary simulation."
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

    private static List<EchoNativeBoundaryFailureCase> failureCases() {
        return List.of(
                failureCase("descriptor_failure", "descriptor_discovery", "ECHO-NATIVE-DESCRIPTOR-INVALID", "descriptor.parse"),
                failureCase("lifecycle_step_failure", "lifecycle_simulation", "ECHO-NATIVE-LIFECYCLE-STEP-FAILED", "lifecycle.step"),
                failureCase("service_attach_failure", "service_attach", "ECHO-NATIVE-SERVICE-ATTACH-FAILED", "service.attach"),
                failureCase("classloader_boundary_violation", "classloader_boundary", "ECHO-NATIVE-CLASSLOADER-BOUNDARY-VIOLATION", "production.classloader"),
                failureCase("transform_request_violation", "transform_policy", "ECHO-NATIVE-TRANSFORMS-BLOCKED", "bytecode.transforms"),
                failureCase("registry_injection_request_violation", "registry_policy", "ECHO-NATIVE-REGISTRY-INJECTION-BLOCKED", "registry.injection")
        );
    }

    private static EchoNativeBoundaryFailureCase failureCase(String id, String source, String diagnosticCode, String blockedCapability) {
        return new EchoNativeBoundaryFailureCase(
                id,
                source,
                diagnosticCode,
                blockedCapability,
                true,
                true,
                false,
                false
        );
    }

    private static Map<String, Object> crashBoundarySimulationResult(
            String packId,
            EchoNativeCrashBoundarySimulationResult result,
            List<EchoNativeDiagnostic> diagnostics,
            List<EchoNativeBoundaryFailureCase> cases
    ) {
        Map<String, Object> data = base("phase13_crash_boundary_simulation_result", diagnostics);
        data.put("capturedDiagnostics", result.capturedDiagnostics());
        data.put("classloaderCreated", result.classloaderCreated());
        data.put("crashBoundaryActive", result.crashBoundaryActive());
        data.put("mutatedState", result.mutatedState());
        data.put("packId", packId);
        data.put("processLaunched", result.processLaunched());
        data.put("simulated", result.simulated());
        data.put("simulatedFailureCaseCount", result.simulatedFailureCases().size());
        data.put("simulatedFailureCases", result.simulatedFailureCases());
        data.put("simulationId", result.simulationId());
        data.put("terminatedProcess", result.terminatedProcess());
        data.put("totalPlannedFailureCases", cases.size());
        data.put("summary", result.simulated()
                ? "Crash-boundary failure cases were simulated as deterministic report data without throwing runtime failures."
                : "Crash-boundary simulation was blocked by upstream Phase 13 diagnostics.");
        return data;
    }

    private static Map<String, Object> boundaryFailureCases(
            String packId,
            List<EchoNativeBoundaryFailureCase> cases,
            List<EchoNativeDiagnostic> diagnostics,
            boolean simulated
    ) {
        Map<String, Object> data = base("phase13_boundary_failure_cases", diagnostics);
        data.put("caseCount", simulated ? cases.size() : 0);
        data.put("cases", simulated ? cases.stream().map(EchoNativePhase13CrashBoundarySimulator::failureCaseMap).toList() : List.of());
        data.put("packId", packId);
        data.put("simulated", simulated);
        data.put("summary", simulated
                ? "All planned boundary failure cases are contained, diagnostic-only, and non-mutating."
                : "Boundary failure case simulation was blocked by upstream Phase 13 diagnostics.");
        return data;
    }

    private static Map<String, Object> classloaderBoundaryRehearsal(
            String packId,
            EchoNativeClassloaderBoundaryRehearsal rehearsal,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_classloader_boundary_rehearsal", diagnostics);
        data.put("blockedCapabilities", rehearsal.blockedCapabilities());
        data.put("classloaderCreated", rehearsal.classloaderCreated());
        data.put("packId", packId);
        data.put("processLaunched", rehearsal.processLaunched());
        data.put("productionClassloader", rehearsal.productionClassloader());
        data.put("rehearsalId", rehearsal.rehearsalId());
        data.put("rehearsed", rehearsal.rehearsed());
        data.put("rehearsedBoundaries", rehearsal.rehearsedBoundaries());
        data.put("resolvesRuntimeClasses", rehearsal.resolvesRuntimeClasses());
        data.put("summary", rehearsal.rehearsed()
                ? "Classloader boundaries were rehearsed as data only; no classloader or runtime class resolution occurred."
                : "Classloader boundary rehearsal was blocked by upstream Phase 13 diagnostics.");
        return data;
    }

    private static Map<String, Object> failureCaseMap(EchoNativeBoundaryFailureCase failureCase) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("blockedCapability", failureCase.blockedCapability());
        data.put("capturedDiagnostic", failureCase.capturedDiagnostic());
        data.put("contained", failureCase.contained());
        data.put("expectedDiagnosticCode", failureCase.expectedDiagnosticCode());
        data.put("id", failureCase.id());
        data.put("mutatedState", failureCase.mutatedState());
        data.put("source", failureCase.source());
        data.put("terminatedProcess", failureCase.terminatedProcess());
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
