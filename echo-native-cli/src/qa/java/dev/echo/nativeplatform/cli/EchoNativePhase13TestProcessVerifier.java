package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeControlledTestProcessPreflight;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativePhase13M1SafetyStatus;
import dev.echo.nativeplatform.contracts.EchoNativeTestProcessBoundaryVerification;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoNativePhase13TestProcessVerifier {
    EchoNativePhase13TestProcessVerificationOutcome verify(
            String packId,
            Path fixture,
            Path phase13PlanPath,
            Path testProcessPlanPath,
            Path loaderBoundaryVerificationPath,
            Path classpathCompatibilityPath
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Object> phase13Plan = readRequiredReport(phase13PlanPath, fixture, packId, "ECHO-NATIVE-PHASE13-PLAN-MISSING", "Phase 13 plan report missing", diagnostics);
        Map<String, Object> testProcessPlan = readRequiredReport(testProcessPlanPath, fixture, packId, "ECHO-NATIVE-TEST-PROCESS-PLAN-MISSING", "Test-process plan report missing", diagnostics);
        Map<String, Object> loaderBoundaryVerification = readRequiredReport(loaderBoundaryVerificationPath, fixture, packId, "ECHO-NATIVE-LOADER-BOUNDARY-VERIFICATION-MISSING", "Loader boundary verification report missing", diagnostics);
        Map<String, Object> classpathCompatibility = readRequiredReport(classpathCompatibilityPath, fixture, packId, "ECHO-NATIVE-CLASSPATH-COMPATIBILITY-MISSING", "Classpath/classloader compatibility report missing", diagnostics);

        Map<String, Object> phase13Data = EchoNativeJson.asObject(phase13Plan.get("data"));
        Map<String, Object> testProcessData = EchoNativeJson.asObject(testProcessPlan.get("data"));
        Map<String, Object> boundaryData = EchoNativeJson.asObject(loaderBoundaryVerification.get("data"));
        Map<String, Object> compatibilityData = EchoNativeJson.asObject(classpathCompatibility.get("data"));

        checkPhase13Plan(phase13Plan, phase13Data, phase13PlanPath, packId, diagnostics);
        checkTestProcessPlan(testProcessPlan, testProcessData, testProcessPlanPath, packId, diagnostics);
        checkLoaderBoundaryVerification(loaderBoundaryVerification, boundaryData, loaderBoundaryVerificationPath, packId, diagnostics);
        checkClasspathCompatibility(classpathCompatibility, compatibilityData, classpathCompatibilityPath, packId, diagnostics);

        diagnostics = unique(diagnostics);
        boolean verified = diagnostics.isEmpty();
        List<String> checkedInputs = List.of(
                "phase13-plan.json",
                "test-process-plan.json",
                "loader-boundary-verification.json",
                "classpath-classloader-compatibility.json"
        );
        List<String> completedChecks = List.of(
                "phase13_plan_gate",
                "test_process_plan_gate",
                "loader_boundary_verification_gate",
                "classpath_classloader_compatibility_gate",
                "process_free_rehearsal_gate"
        );
        List<String> blockedTargets = stringList(testProcessData.get("blockedTargets"));
        List<String> plannedTargets = stringList(testProcessData.get("allowedTargets"));
        EchoNativeTestProcessBoundaryVerification boundaryVerification = new EchoNativeTestProcessBoundaryVerification(
                "phase13.test_process.boundary.verification",
                verified,
                false,
                false,
                false,
                false,
                false,
                false,
                verified ? checkedInputs : List.of(),
                blockedTargets
        );
        EchoNativeControlledTestProcessPreflight preflight = new EchoNativeControlledTestProcessPreflight(
                "phase13.controlled_test_process.preflight",
                verified,
                false,
                false,
                false,
                false,
                verified ? plannedTargets : List.of(),
                blockedTargets
        );
        EchoNativePhase13M1SafetyStatus safetyStatus = new EchoNativePhase13M1SafetyStatus(
                "phase13.m1.safety.status",
                verified,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                verified ? completedChecks : List.of()
        );

        return new EchoNativePhase13TestProcessVerificationOutcome(
                packId,
                testProcessBoundaryVerification(packId, boundaryVerification, diagnostics),
                controlledTestProcessPreflight(packId, preflight, diagnostics),
                phase13M1SafetyStatus(packId, safetyStatus, diagnostics),
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
                    "Phase 13 plan is not ready for test-process boundary verification",
                    "Test-process boundary verification requires a PASS phase13-plan.json with no runtime work started.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Run echo-native phase13 plan for a fixture with PASS Phase 13 readiness first."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static void checkTestProcessPlan(
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
        boolean planOnly = Boolean.TRUE.equals(data.get("planOnly"));
        boolean processLaunchAllowed = Boolean.TRUE.equals(data.get("processLaunchAllowed"));
        boolean gameLaunchAllowed = Boolean.TRUE.equals(data.get("gameLaunchAllowed"));
        boolean subprocessCreated = Boolean.TRUE.equals(data.get("subprocessCreated"));
        if (!pass || !planOnly || processLaunchAllowed || gameLaunchAllowed || subprocessCreated) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-TEST-PROCESS-PLAN-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Test-process plan is not safe for boundary verification",
                    "Test-process boundary verification requires a PASS test-process-plan.json with processLaunchAllowed=false, gameLaunchAllowed=false, and subprocessCreated=false.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Regenerate Phase 13 planning and keep test-process work plan-only."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static void checkLoaderBoundaryVerification(
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
        boolean verified = Boolean.TRUE.equals(data.get("verified"));
        boolean classloaderCreated = Boolean.TRUE.equals(data.get("classloaderCreated"));
        boolean resolvesRuntimeClasses = Boolean.TRUE.equals(data.get("resolvesRuntimeClasses"));
        boolean processLaunched = Boolean.TRUE.equals(data.get("processLaunched"));
        boolean mutatedFilesystem = Boolean.TRUE.equals(data.get("mutatedFilesystem"));
        if (!pass || !verified || classloaderCreated || resolvesRuntimeClasses || processLaunched || mutatedFilesystem) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-LOADER-BOUNDARY-VERIFICATION-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Loader boundary verification is not safe for test-process rehearsal",
                    "Test-process boundary verification requires PASS loader-boundary-verification.json with no classloader, runtime class resolution, process launch, or filesystem mutation.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Run echo-native phase13 verify boundaries before test-process boundary verification."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static void checkClasspathCompatibility(
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
        boolean compatible = Boolean.TRUE.equals(data.get("compatible"));
        boolean classpathPlannedOnly = Boolean.TRUE.equals(data.get("classpathPlannedOnly"));
        boolean classloaderCreated = Boolean.TRUE.equals(data.get("classloaderCreated"));
        boolean productionClassloader = Boolean.TRUE.equals(data.get("productionClassloader"));
        boolean resolvesRuntimeClasses = Boolean.TRUE.equals(data.get("resolvesRuntimeClasses"));
        boolean processLaunched = Boolean.TRUE.equals(data.get("processLaunched"));
        boolean filesystemMutated = Boolean.TRUE.equals(data.get("filesystemMutated"));
        if (!pass || !compatible || !classpathPlannedOnly || classloaderCreated || productionClassloader || resolvesRuntimeClasses || processLaunched || filesystemMutated) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-CLASSPATH-COMPATIBILITY-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Classpath/classloader compatibility is not safe for test-process rehearsal",
                    "Test-process boundary verification requires PASS classpath-classloader-compatibility.json with planned classpath data only.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Run echo-native phase13 verify boundaries and keep classpath/classloader checks data-only."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static Map<String, Object> testProcessBoundaryVerification(
            String packId,
            EchoNativeTestProcessBoundaryVerification verification,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_test_process_boundary_verification", diagnostics);
        data.put("blockedCapabilities", verification.blockedCapabilities());
        data.put("classloaderCreated", verification.classloaderCreated());
        data.put("commandExecuted", verification.commandExecuted());
        data.put("filesystemMutated", verification.filesystemMutated());
        data.put("gameProcessLaunched", verification.gameProcessLaunched());
        data.put("packId", packId);
        data.put("processLaunched", verification.processLaunched());
        data.put("resolvesRuntimeClasses", verification.resolvesRuntimeClasses());
        data.put("summary", verification.verified()
                ? "Test-process boundary inputs passed and no command or process was executed."
                : "Test-process boundary verification failed because an upstream report was missing, failed, or unsafe.");
        data.put("verificationId", verification.verificationId());
        data.put("verified", verification.verified());
        data.put("verifiedInputs", verification.verifiedInputs());
        return data;
    }

    private static Map<String, Object> controlledTestProcessPreflight(
            String packId,
            EchoNativeControlledTestProcessPreflight preflight,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_controlled_test_process_preflight", diagnostics);
        data.put("commandExecuted", preflight.commandExecuted());
        data.put("filesystemMutated", preflight.filesystemMutated());
        data.put("gameProcessLaunched", preflight.gameProcessLaunched());
        data.put("packId", packId);
        data.put("plannedTargets", preflight.plannedTargets());
        data.put("blockedTargets", preflight.blockedTargets());
        data.put("preflightId", preflight.preflightId());
        data.put("processLaunched", preflight.processLaunched());
        data.put("ready", preflight.ready());
        data.put("summary", preflight.ready()
                ? "Controlled test-process preflight is ready as data only; no process or command was started."
                : "Controlled test-process preflight is blocked by upstream diagnostics.");
        return data;
    }

    private static Map<String, Object> phase13M1SafetyStatus(
            String packId,
            EchoNativePhase13M1SafetyStatus status,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m1_safety_status", diagnostics);
        data.put("classloaderCreated", status.classloaderCreated());
        data.put("commandExecuted", status.commandExecuted());
        data.put("completedM1Checks", status.completedM1Checks());
        data.put("filesystemMutated", status.filesystemMutated());
        data.put("gameProcessLaunched", status.gameProcessLaunched());
        data.put("packId", packId);
        data.put("processLaunched", status.processLaunched());
        data.put("resolvesRuntimeClasses", status.resolvesRuntimeClasses());
        data.put("safeToContinue", status.safeToContinue());
        data.put("statusId", status.statusId());
        data.put("summary", status.safeToContinue()
                ? "Phase 13 M1 remains safe to continue with report-only prototype planning."
                : "Phase 13 M1 safety status is blocked by test-process boundary diagnostics.");
        data.put("unsafeRuntimeWorkStarted", status.unsafeRuntimeWorkStarted());
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
                    "Required Phase 13 test-process boundary input report was not found.",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate Phase 13 planning, boundary verification, and compatibility reports before test-process verification."
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
