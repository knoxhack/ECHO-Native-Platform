package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeControlledLaunchFailureCapturePlan;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIsolatedLaunchEnvironmentPlan;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativeLaunchSafetyGate;
import dev.echo.nativeplatform.contracts.EchoNativeMinecraftLaunchPreflight;
import dev.echo.nativeplatform.contracts.EchoNativePackProfile;
import dev.echo.nativeplatform.contracts.EchoNativePhase13M17Readiness;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoNativeLaunchPreflightVerifier {
    EchoNativeLaunchPreflightOutcome verify(
            String packId,
            Path fixture,
            EchoNativePackProfile packProfile,
            Map<String, Path> requiredReports
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        for (Map.Entry<String, Path> entry : requiredReports.entrySet()) {
            Map<String, Object> report = readRequiredReport(entry.getValue(), fixture, packId, entry.getKey(), diagnostics);
            checkReport(entry.getKey(), report, entry.getValue(), packId, diagnostics);
        }

        diagnostics = unique(diagnostics);
        boolean passed = diagnostics.isEmpty();
        String minecraftVersion = packProfile == null ? "unknown" : packProfile.minecraftVersion();
        List<String> completedChecks = passed ? List.of(
                "m16_safety_status_pass",
                "failure_containment_ready",
                "support_bundle_plan_ready",
                "launch_arguments_planned",
                "classpath_planned",
                "native_extraction_planned_only",
                "isolated_environment_planned",
                "controlled_failure_capture_planned"
        ) : List.of();

        EchoNativeIsolatedLaunchEnvironmentPlan isolatedPlan = new EchoNativeIsolatedLaunchEnvironmentPlan(
                "phase13.m17.isolated.launch.environment.plan",
                passed,
                passed,
                false,
                false,
                false,
                false,
                false,
                "tmp/echo-native/phase13/m17/" + safePackId(packId),
                passed ? isolationRules() : List.of()
        );
        EchoNativeMinecraftLaunchPreflight launchPreflight = new EchoNativeMinecraftLaunchPreflight(
                "phase13.m17.minecraft.launch.preflight",
                passed,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                minecraftVersion,
                passed ? List.copyOf(requiredReports.keySet()) : List.of()
        );
        EchoNativeControlledLaunchFailureCapturePlan failureCapturePlan = new EchoNativeControlledLaunchFailureCapturePlan(
                "phase13.m17.controlled.launch.failure.capture.plan",
                passed,
                true,
                true,
                false,
                false,
                false,
                false,
                passed ? failureSignals() : List.of()
        );
        EchoNativeLaunchSafetyGate safetyGate = new EchoNativeLaunchSafetyGate(
                "phase13.m17.launch.safety.gate",
                passed,
                passed,
                passed,
                passed,
                false,
                false,
                false,
                false,
                false,
                false,
                completedChecks
        );
        EchoNativePhase13M17Readiness readiness = new EchoNativePhase13M17Readiness(
                "phase13.m17.readiness",
                passed,
                passed,
                passed,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                completedChecks
        );

        return new EchoNativeLaunchPreflightOutcome(
                packId,
                isolatedLaunchEnvironmentPlan(packId, isolatedPlan, diagnostics),
                minecraftLaunchPreflight(packId, launchPreflight, diagnostics),
                launchSafetyGate(packId, safetyGate, diagnostics),
                controlledLaunchFailureCapturePlan(packId, failureCapturePlan, diagnostics),
                phase13M17Readiness(packId, readiness, diagnostics),
                diagnostics
        );
    }

    private static void checkReport(
            String reportName,
            Map<String, Object> report,
            Path path,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        if (report.isEmpty()) {
            return;
        }
        Map<String, Object> data = EchoNativeJson.asObject(report.get("data"));
        if (!"PASS".equals(report.get("status"))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-M17-UPSTREAM-REPORT-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M17 launch preflight upstream report is not PASS",
                    "Launch preflight requires PASS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Regenerate and resolve the Phase 13 report chain through M16 before M17 preflight."
            ));
        }
        if (hasUnsafeRuntimeWork(data)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-M17-SAFETY-VIOLATION",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M17 upstream report contains unsafe runtime work",
                    reportName + " indicates runtime work that is still blocked during launch preflight.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Keep M17.0 preflight report-only: no launch, command execution, classloader, downloads, extraction, transforms, registry injection, or filesystem mutation."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static Map<String, Object> isolatedLaunchEnvironmentPlan(
            String packId,
            EchoNativeIsolatedLaunchEnvironmentPlan plan,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m17_isolated_launch_environment_plan", diagnostics);
        data.put("configMutationAllowed", plan.configMutationAllowed());
        data.put("filesystemMutated", plan.filesystemMutated());
        data.put("isolatedDirectoryPlanned", plan.isolatedDirectoryPlanned());
        data.put("isolationRules", plan.isolationRules());
        data.put("launchPreflightComplete", plan.launchPreflightComplete());
        data.put("packId", packId);
        data.put("packMutationAllowed", plan.packMutationAllowed());
        data.put("planId", plan.planId());
        data.put("plannedWorkingDirectory", plan.plannedWorkingDirectory());
        data.put("saveMutationAllowed", plan.saveMutationAllowed());
        data.put("summary", plan.launchPreflightComplete()
                ? "M17 isolated launch environment is planned without mutating user installs, packs, saves, or configs."
                : "M17 isolated launch environment planning is blocked by upstream diagnostics.");
        data.put("userInstallMutationAllowed", plan.userInstallMutationAllowed());
        return data;
    }

    private static Map<String, Object> minecraftLaunchPreflight(
            String packId,
            EchoNativeMinecraftLaunchPreflight preflight,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m17_minecraft_launch_preflight", diagnostics);
        data.put("classloaderCreated", preflight.classloaderCreated());
        data.put("commandExecuted", preflight.commandExecuted());
        data.put("gameProcessLaunched", preflight.gameProcessLaunched());
        data.put("launchPreflightComplete", preflight.launchPreflightComplete());
        data.put("libraryDownloadStarted", preflight.libraryDownloadStarted());
        data.put("minecraftVersion", preflight.minecraftVersion());
        data.put("nativeExtractionStarted", preflight.nativeExtractionStarted());
        data.put("packId", packId);
        data.put("preflightId", preflight.preflightId());
        data.put("processLaunched", preflight.processLaunched());
        data.put("requiredInputs", preflight.requiredInputs());
        data.put("summary", preflight.launchPreflightComplete()
                ? "M17 Minecraft launch inputs are ready for a future isolated attempt; this command did not launch a process."
                : "M17 Minecraft launch preflight is blocked by upstream diagnostics.");
        data.put("transformsEnabled", preflight.transformsEnabled());
        return data;
    }

    private static Map<String, Object> launchSafetyGate(
            String packId,
            EchoNativeLaunchSafetyGate gate,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m17_launch_safety_gate", diagnostics);
        data.put("completedChecks", gate.completedChecks());
        data.put("failureCapturePlanned", gate.failureCapturePlanned());
        data.put("gateId", gate.gateId());
        data.put("isolatedEnvironmentPassed", gate.isolatedEnvironmentPassed());
        data.put("m16SafetyPassed", gate.m16SafetyPassed());
        data.put("packId", packId);
        data.put("safeForIsolatedLaunchAttempt", gate.safeForIsolatedLaunchAttempt());
        data.put("summary", gate.safeForIsolatedLaunchAttempt()
                ? "M17 safety gate allows the next slice to attempt an isolated launch, but this preflight did not launch Minecraft."
                : "M17 safety gate blocks isolated launch until upstream diagnostics are resolved.");
        return data;
    }

    private static Map<String, Object> controlledLaunchFailureCapturePlan(
            String packId,
            EchoNativeControlledLaunchFailureCapturePlan plan,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m17_controlled_launch_failure_capture_plan", diagnostics);
        data.put("diagnosticsCaptured", plan.diagnosticsCaptured());
        data.put("packId", packId);
        data.put("planId", plan.planId());
        data.put("planned", plan.planned());
        data.put("plannedSignals", plan.plannedSignals());
        data.put("summary", plan.planned()
                ? "M17 controlled launch failure capture is planned before any real Minecraft process is allowed."
                : "M17 controlled launch failure capture planning is blocked by upstream diagnostics.");
        data.put("supportBundlePlannedOnly", plan.supportBundlePlannedOnly());
        return data;
    }

    private static Map<String, Object> phase13M17Readiness(
            String packId,
            EchoNativePhase13M17Readiness readiness,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m17_readiness", diagnostics);
        data.put("classloaderCreated", readiness.classloaderCreated());
        data.put("completedChecks", readiness.completedChecks());
        data.put("gameClassesResolved", readiness.gameClassesResolved());
        data.put("launchPreflightComplete", readiness.launchPreflightComplete());
        data.put("libraryDownloadStarted", readiness.libraryDownloadStarted());
        data.put("nativeExtractionStarted", readiness.nativeExtractionStarted());
        data.put("packId", packId);
        data.put("phase13M17Ready", readiness.phase13M17Ready());
        data.put("readinessId", readiness.readinessId());
        data.put("safeForIsolatedLaunchAttempt", readiness.safeForIsolatedLaunchAttempt());
        data.put("summary", readiness.phase13M17Ready()
                ? "M17 preflight is ready for the next isolated launch-attempt slice; no launch occurred in this command."
                : "M17 preflight is blocked by upstream diagnostics.");
        return data;
    }

    private static Map<String, Object> base(String phase, List<EchoNativeDiagnostic> diagnostics) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("bytecodeMutated", false);
        data.put("cacheMutated", false);
        data.put("classloaderCreated", false);
        data.put("commandExecuted", false);
        data.put("diagnosticCount", diagnostics.size());
        data.put("diagnosticsCaptured", true);
        data.put("downloadAllowed", false);
        data.put("dryRunOnly", true);
        data.put("filesystemMutated", false);
        data.put("gameClassesResolved", false);
        data.put("gameProcessLaunched", false);
        data.put("libraryDownloadStarted", false);
        data.put("minecraftLaunched", false);
        data.put("nativeExtractionStarted", false);
        data.put("phase", phase);
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("reportOnly", true);
        data.put("supportBundlePlannedOnly", true);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
    }

    private static List<String> isolationRules() {
        return List.of(
                "use_planned_temp_workspace_only",
                "do_not_touch_user_installs",
                "do_not_write_saves",
                "do_not_mutate_pack_configs",
                "capture_failure_reports_before_launch_attempt"
        );
    }

    private static List<String> failureSignals() {
        return List.of(
                "exit_code",
                "timeout",
                "stdout_tail",
                "stderr_tail",
                "generated_reports",
                "support_bundle_plan"
        );
    }

    private static Map<String, Object> readRequiredReport(
            Path reportPath,
            Path fixture,
            String packId,
            String reportName,
            List<EchoNativeDiagnostic> diagnostics
    ) throws IOException {
        if (!Files.isRegularFile(reportPath)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-M17-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M17 required report missing",
                    "Launch preflight requires " + reportName + ".",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate the Phase 13 report chain through M16 before running M17 preflight."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
    }

    private static boolean hasUnsafeRuntimeWork(Map<String, Object> data) {
        return Boolean.TRUE.equals(data.get("remoteManifestDownloaded"))
                || Boolean.TRUE.equals(data.get("libraryDownloadStarted"))
                || Boolean.TRUE.equals(data.get("cacheMutated"))
                || Boolean.TRUE.equals(data.get("nativeExtractionStarted"))
                || Boolean.TRUE.equals(data.get("nativeFilesExtracted"))
                || Boolean.TRUE.equals(data.get("processLaunched"))
                || Boolean.TRUE.equals(data.get("gameProcessLaunched"))
                || Boolean.TRUE.equals(data.get("minecraftLaunched"))
                || Boolean.TRUE.equals(data.get("commandExecuted"))
                || Boolean.TRUE.equals(data.get("classloaderCreated"))
                || Boolean.TRUE.equals(data.get("productionClassloader"))
                || Boolean.TRUE.equals(data.get("resolvesRuntimeClasses"))
                || Boolean.TRUE.equals(data.get("gameClassesResolved"))
                || Boolean.TRUE.equals(data.get("minecraftClassesResolved"))
                || Boolean.TRUE.equals(data.get("addonCodeExecuted"))
                || Boolean.TRUE.equals(data.get("realAddonCodeExecuted"))
                || Boolean.TRUE.equals(data.get("serviceCodeExecuted"))
                || Boolean.TRUE.equals(data.get("resourceRuntimeAccessed"))
                || Boolean.TRUE.equals(data.get("minecraftResourceManagerTouched"))
                || Boolean.TRUE.equals(data.get("minecraftRegistryTouched"))
                || Boolean.TRUE.equals(data.get("registryInjected"))
                || Boolean.TRUE.equals(data.get("registryMutated"))
                || Boolean.TRUE.equals(data.get("liveNetworkingStarted"))
                || Boolean.TRUE.equals(data.get("socketOpened"))
                || Boolean.TRUE.equals(data.get("clientConnectionOpened"))
                || Boolean.TRUE.equals(data.get("serverConnectionOpened"))
                || Boolean.TRUE.equals(data.get("packetSent"))
                || Boolean.TRUE.equals(data.get("packetReceived"))
                || Boolean.TRUE.equals(data.get("transformsEnabled"))
                || Boolean.TRUE.equals(data.get("transformsPerformed"))
                || Boolean.TRUE.equals(data.get("minecraftBytecodeTransformed"))
                || Boolean.TRUE.equals(data.get("addonBytecodeTransformed"))
                || Boolean.TRUE.equals(data.get("bytecodeMutated"))
                || Boolean.TRUE.equals(data.get("filesystemMutated"))
                || Boolean.TRUE.equals(data.get("unsafeRuntimeWorkStarted"));
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

    private static String safePackId(String packId) {
        return packId == null || packId.isBlank() ? "unknown" : packId;
    }
}
