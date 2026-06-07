package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeClasspathClassloaderCompatibility;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativeLoaderBoundaryState;
import dev.echo.nativeplatform.contracts.EchoNativeLoaderBoundaryVerificationResult;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoNativePhase13BoundaryVerifier {
    EchoNativePhase13BoundaryVerificationOutcome verify(
            String packId,
            Path fixture,
            Path phase13PlanPath,
            Path lifecycleResultPath,
            Path serviceAttachResultPath,
            Path crashBoundarySimulationPath,
            Path classloaderBoundaryRehearsalPath,
            Path classpathPlanPath
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Object> phase13Plan = readRequiredReport(phase13PlanPath, fixture, packId, "ECHO-NATIVE-PHASE13-PLAN-MISSING", "Phase 13 plan report missing", diagnostics);
        Map<String, Object> lifecycleResult = readRequiredReport(lifecycleResultPath, fixture, packId, "ECHO-NATIVE-LIFECYCLE-RESULT-MISSING", "Lifecycle simulation result missing", diagnostics);
        Map<String, Object> serviceAttachResult = readRequiredReport(serviceAttachResultPath, fixture, packId, "ECHO-NATIVE-SERVICE-ATTACH-RESULT-MISSING", "Service attach simulation result missing", diagnostics);
        Map<String, Object> crashBoundarySimulation = readRequiredReport(crashBoundarySimulationPath, fixture, packId, "ECHO-NATIVE-CRASH-BOUNDARY-SIMULATION-MISSING", "Crash-boundary simulation report missing", diagnostics);
        Map<String, Object> classloaderBoundaryRehearsal = readRequiredReport(classloaderBoundaryRehearsalPath, fixture, packId, "ECHO-NATIVE-CLASSLOADER-REHEARSAL-MISSING", "Classloader boundary rehearsal report missing", diagnostics);
        Map<String, Object> classpathPlan = readRequiredReport(classpathPlanPath, fixture, packId, "ECHO-NATIVE-CLASSPATH-PLAN-MISSING", "Classpath plan report missing", diagnostics);

        Map<String, Object> phase13Data = EchoNativeJson.asObject(phase13Plan.get("data"));
        Map<String, Object> lifecycleData = EchoNativeJson.asObject(lifecycleResult.get("data"));
        Map<String, Object> serviceData = EchoNativeJson.asObject(serviceAttachResult.get("data"));
        Map<String, Object> crashData = EchoNativeJson.asObject(crashBoundarySimulation.get("data"));
        Map<String, Object> rehearsalData = EchoNativeJson.asObject(classloaderBoundaryRehearsal.get("data"));
        Map<String, Object> classpathData = EchoNativeJson.asObject(classpathPlan.get("data"));

        checkPhase13Plan(phase13Plan, phase13Data, phase13PlanPath, packId, diagnostics);
        checkLifecycleResult(lifecycleResult, lifecycleData, lifecycleResultPath, packId, diagnostics);
        checkServiceAttachResult(serviceAttachResult, serviceData, serviceAttachResultPath, packId, diagnostics);
        checkCrashBoundarySimulation(crashBoundarySimulation, crashData, crashBoundarySimulationPath, packId, diagnostics);
        checkClassloaderBoundaryRehearsal(classloaderBoundaryRehearsal, rehearsalData, classloaderBoundaryRehearsalPath, packId, diagnostics);
        checkClasspathPlan(classpathPlan, classpathData, classpathPlanPath, packId, diagnostics);

        diagnostics = unique(diagnostics);
        boolean verified = diagnostics.isEmpty();
        List<EchoNativeLoaderBoundaryState> states = boundaryStates(verified);
        EchoNativeLoaderBoundaryVerificationResult verification = new EchoNativeLoaderBoundaryVerificationResult(
                "phase13.loader.boundary.verification",
                verified,
                false,
                false,
                false,
                false,
                verified ? states.stream().map(EchoNativeLoaderBoundaryState::id).toList() : List.of()
        );
        EchoNativeClasspathClassloaderCompatibility compatibility = new EchoNativeClasspathClassloaderCompatibility(
                "phase13.classpath.classloader.compatibility",
                verified,
                true,
                false,
                false,
                false,
                false,
                false,
                classpathEntries(classpathData).size(),
                List.of("classpath-plan.json", "classloader-boundary-rehearsal.json")
        );

        return new EchoNativePhase13BoundaryVerificationOutcome(
                packId,
                loaderBoundaryStateMachine(packId, states, diagnostics, verified),
                loaderBoundaryVerification(packId, verification, diagnostics),
                classpathClassloaderCompatibility(packId, compatibility, diagnostics, classpathEntries(classpathData)),
                diagnostics
        );
    }

    private static void checkPhase13Plan(
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
        boolean planningStarted = Boolean.TRUE.equals(data.get("phase13PlanningStarted"));
        boolean planOnly = Boolean.TRUE.equals(data.get("planOnly"));
        boolean prototypeRuntimeStarted = Boolean.TRUE.equals(data.get("prototypeRuntimeStarted"));
        boolean unsafeRuntimeWorkStarted = Boolean.TRUE.equals(data.get("unsafeRuntimeWorkStarted"));
        if (!pass || !planningStarted || !planOnly || prototypeRuntimeStarted || unsafeRuntimeWorkStarted) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-PHASE13-PLAN-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 plan is not ready for boundary verification",
                    "Boundary verification requires a PASS phase13-plan.json with plan-only prototype work and no runtime behavior started.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Run echo-native phase13 plan for a fixture with PASS Phase 13 readiness first."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static void checkLifecycleResult(
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
        boolean simulated = Boolean.TRUE.equals(data.get("simulated"));
        boolean simulationOnly = Boolean.TRUE.equals(data.get("simulationOnly"));
        boolean classloaderCreated = Boolean.TRUE.equals(data.get("classloaderCreated"));
        boolean processLaunched = Boolean.TRUE.equals(data.get("processLaunched"));
        if (!pass || !simulated || !simulationOnly || classloaderCreated || processLaunched) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-LIFECYCLE-SIMULATION-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Lifecycle simulation is not ready for boundary verification",
                    "Boundary verification requires a PASS lifecycle-simulation-result.json with classloaderCreated=false and processLaunched=false.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Run echo-native phase13 simulate lifecycle before boundary verification."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static void checkServiceAttachResult(
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
        boolean simulated = Boolean.TRUE.equals(data.get("serviceAttachSimulated"));
        boolean simulationOnly = Boolean.TRUE.equals(data.get("simulationOnly"));
        boolean executedServiceCode = Boolean.TRUE.equals(data.get("executedServiceCode"));
        boolean classloaderCreated = Boolean.TRUE.equals(data.get("classloaderCreated"));
        boolean processLaunched = Boolean.TRUE.equals(data.get("processLaunched"));
        if (!pass || !simulated || !simulationOnly || executedServiceCode || classloaderCreated || processLaunched) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-SERVICE-SIMULATION-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Service attach simulation is not ready for boundary verification",
                    "Boundary verification requires a PASS service-attach-simulation-result.json with no service code execution, classloader, or process launch.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Run echo-native phase13 simulate services before boundary verification."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static void checkCrashBoundarySimulation(
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
        boolean simulated = Boolean.TRUE.equals(data.get("simulated"));
        boolean crashBoundaryActive = Boolean.TRUE.equals(data.get("crashBoundaryActive"));
        boolean classloaderCreated = Boolean.TRUE.equals(data.get("classloaderCreated"));
        boolean processLaunched = Boolean.TRUE.equals(data.get("processLaunched"));
        boolean terminatedProcess = Boolean.TRUE.equals(data.get("terminatedProcess"));
        boolean mutatedState = Boolean.TRUE.equals(data.get("mutatedState"));
        if (!pass || !simulated || !crashBoundaryActive || classloaderCreated || processLaunched || terminatedProcess || mutatedState) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-CRASH-BOUNDARY-SIMULATION-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Crash-boundary simulation is not ready for boundary verification",
                    "Boundary verification requires a PASS crash-boundary-simulation-result.json with contained, non-mutating failure cases.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Run echo-native phase13 simulate crash-boundary before boundary verification."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static void checkClassloaderBoundaryRehearsal(
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
        boolean rehearsed = Boolean.TRUE.equals(data.get("rehearsed"));
        boolean classloaderCreated = Boolean.TRUE.equals(data.get("classloaderCreated"));
        boolean productionClassloader = Boolean.TRUE.equals(data.get("productionClassloader"));
        boolean resolvesRuntimeClasses = Boolean.TRUE.equals(data.get("resolvesRuntimeClasses"));
        boolean processLaunched = Boolean.TRUE.equals(data.get("processLaunched"));
        if (!pass || !rehearsed || classloaderCreated || productionClassloader || resolvesRuntimeClasses || processLaunched) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-CLASSLOADER-REHEARSAL-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Classloader boundary rehearsal is not ready for boundary verification",
                    "Boundary verification requires a PASS classloader-boundary-rehearsal.json with classloaderCreated=false and resolvesRuntimeClasses=false.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Regenerate the crash-boundary simulation and keep classloader work data-only."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static void checkClasspathPlan(
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
        boolean classloaderCreated = Boolean.TRUE.equals(data.get("classloaderCreated"));
        boolean allPlanned = classpathEntries(data).stream().allMatch(entry -> entry.startsWith("planned://"));
        if (!pass || classloaderCreated || !allPlanned) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-CLASSPATH-COMPATIBILITY-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Classpath plan is not compatible with data-only boundary verification",
                    "Boundary verification requires a PASS classpath-plan.json with planned:// entries and classloaderCreated=false.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Regenerate bootstrap --dry-run and keep classpath entries as planned descriptors only."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static Map<String, Object> loaderBoundaryStateMachine(
            String packId,
            List<EchoNativeLoaderBoundaryState> states,
            List<EchoNativeDiagnostic> diagnostics,
            boolean verified
    ) {
        Map<String, Object> data = base("phase13_loader_boundary_state_machine", diagnostics);
        data.put("packId", packId);
        data.put("stateCount", states.size());
        data.put("states", states.stream().map(EchoNativePhase13BoundaryVerifier::stateMap).toList());
        data.put("stateMachineId", "phase13.loader.boundary.state_machine");
        data.put("summary", verified
                ? "Loader boundaries were verified as a deterministic data-only state machine."
                : "Loader boundary state machine verification was blocked by upstream Phase 13 diagnostics.");
        data.put("verified", verified);
        return data;
    }

    private static Map<String, Object> loaderBoundaryVerification(
            String packId,
            EchoNativeLoaderBoundaryVerificationResult result,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_loader_boundary_verification", diagnostics);
        data.put("classloaderCreated", result.classloaderCreated());
        data.put("mutatedFilesystem", result.mutatedFilesystem());
        data.put("packId", packId);
        data.put("processLaunched", result.processLaunched());
        data.put("resolvesRuntimeClasses", result.resolvesRuntimeClasses());
        data.put("summary", result.verified()
                ? "All Phase 13 M1 boundary inputs passed and no runtime boundary was crossed."
                : "Boundary verification failed because one or more required reports were missing, failed, or unsafe.");
        data.put("verificationId", result.verificationId());
        data.put("verified", result.verified());
        data.put("verifiedStates", result.verifiedStates());
        return data;
    }

    private static Map<String, Object> classpathClassloaderCompatibility(
            String packId,
            EchoNativeClasspathClassloaderCompatibility compatibility,
            List<EchoNativeDiagnostic> diagnostics,
            List<String> classpathEntries
    ) {
        Map<String, Object> data = base("phase13_classpath_classloader_compatibility", diagnostics);
        data.put("checkedInputs", compatibility.checkedInputs());
        data.put("classpathEntries", classpathEntries);
        data.put("classpathEntryCount", compatibility.classpathEntryCount());
        data.put("classpathPlannedOnly", compatibility.classpathPlannedOnly());
        data.put("classloaderCreated", compatibility.classloaderCreated());
        data.put("compatibilityId", compatibility.compatibilityId());
        data.put("compatible", compatibility.compatible());
        data.put("filesystemMutated", compatibility.filesystemMutated());
        data.put("packId", packId);
        data.put("processLaunched", compatibility.processLaunched());
        data.put("productionClassloader", compatibility.productionClassloader());
        data.put("resolvesRuntimeClasses", compatibility.resolvesRuntimeClasses());
        data.put("summary", compatibility.compatible()
                ? "Classpath entries remain planned descriptors and no classloader or runtime class resolution occurred."
                : "Classpath/classloader compatibility failed because an input report was missing, failed, or unsafe.");
        return data;
    }

    private static Map<String, Object> stateMap(EchoNativeLoaderBoundaryState state) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("classloaderCreated", state.classloaderCreated());
        data.put("id", state.id());
        data.put("mutatedState", state.mutatedState());
        data.put("processLaunched", state.processLaunched());
        data.put("requiredInput", state.requiredInput());
        data.put("simulationOnly", state.simulationOnly());
        data.put("summary", state.summary());
        data.put("verified", state.verified());
        return data;
    }

    private static List<EchoNativeLoaderBoundaryState> boundaryStates(boolean verified) {
        return List.of(
                state("DESCRIPTOR_BOUNDARY", "phase13-plan.json", verified, "Descriptor metadata stays outside runtime class resolution."),
                state("LIFECYCLE_BOUNDARY", "lifecycle-simulation-result.json", verified, "Lifecycle order is simulated in memory only."),
                state("SERVICE_BOUNDARY", "service-attach-simulation-result.json", verified, "Service attachment order is verified without executing service code."),
                state("CLASSLOADER_BOUNDARY", "classloader-boundary-rehearsal.json", verified, "Classloader behavior remains a rehearsal with no classloader creation."),
                state("CRASH_BOUNDARY", "crash-boundary-simulation-result.json", verified, "Crash handling remains contained diagnostic data."),
                state("TEST_PROCESS_BOUNDARY", "test-process-plan.json", verified, "Test process work remains planned only and no process is launched.")
        );
    }

    private static EchoNativeLoaderBoundaryState state(String id, String requiredInput, boolean verified, String summary) {
        return new EchoNativeLoaderBoundaryState(
                id,
                requiredInput,
                verified,
                true,
                false,
                false,
                false,
                summary
        );
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

    private static List<String> classpathEntries(Map<String, Object> classpathData) {
        return stringList(classpathData.get("entries"));
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
                    "Required Phase 13 boundary verification input report was not found.",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate Phase 13 planning, lifecycle, service, crash-boundary, classloader rehearsal, and bootstrap reports before boundary verification."
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
