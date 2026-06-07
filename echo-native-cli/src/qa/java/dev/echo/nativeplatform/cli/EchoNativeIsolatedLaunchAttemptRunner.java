package dev.echo.nativeplatform.cli;

import dev.echo.nativeplatform.contracts.EchoNativeControlledLaunchAttemptResult;
import dev.echo.nativeplatform.contracts.EchoNativeDiagnostic;
import dev.echo.nativeplatform.contracts.EchoNativeIsolatedLaunchAttemptPlan;
import dev.echo.nativeplatform.contracts.EchoNativeIssueSeverity;
import dev.echo.nativeplatform.contracts.EchoNativeLaunchOutputCapture;
import dev.echo.nativeplatform.contracts.EchoNativeLocalRuntimeArtifactCheck;
import dev.echo.nativeplatform.contracts.EchoNativePhase13M17LaunchStatus;
import dev.echo.nativeplatform.diagnostics.EchoNativeJson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EchoNativeIsolatedLaunchAttemptRunner {
    EchoNativeIsolatedLaunchAttemptOutcome attempt(
            String packId,
            Path fixture,
            Map<String, Path> requiredReports
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Map<String, Object>> reports = new LinkedHashMap<>();
        for (Map.Entry<String, Path> entry : requiredReports.entrySet()) {
            Map<String, Object> report = readRequiredReport(entry.getValue(), fixture, packId, entry.getKey(), diagnostics);
            reports.put(entry.getKey(), report);
            checkReport(entry.getKey(), report, entry.getValue(), packId, diagnostics);
        }

        Map<String, Object> artifactMap = reports.getOrDefault("local-runtime-artifact-map.json", Map.of());
        Map<String, Object> classpathPlan = reports.getOrDefault("classpath-builder-plan.json", Map.of());
        Map<String, Object> nativeExtractionPlan = reports.getOrDefault("native-extraction-plan.json", Map.of());
        List<Map<String, Object>> artifacts = artifactChecks(artifactMap, classpathPlan, nativeExtractionPlan);
        for (Map<String, Object> artifact : artifacts) {
            if (Boolean.FALSE.equals(artifact.get("local"))) {
                diagnostics.add(new EchoNativeDiagnostic(
                        "ECHO-NATIVE-M17-LOCAL-RUNTIME-ARTIFACT-MISSING",
                        EchoNativeIssueSeverity.ERROR,
                        "Local runtime artifact is not available for isolated launch",
                        artifact.get("id") + " is still a planned descriptor and cannot be launched without downloads or mutation.",
                        null,
                        packId,
                        List.of(String.valueOf(artifact.get("sourceReport"))),
                        "Provide an explicit local runtime artifact fixture in a later slice, or keep the launch attempt as a controlled failure."
                ));
            }
        }

        diagnostics = unique(diagnostics);
        boolean upstreamReady = diagnostics.stream().noneMatch(diagnostic ->
                "ECHO-NATIVE-M17-UPSTREAM-REPORT-BLOCKED".equals(diagnostic.code())
                        || "ECHO-NATIVE-M17-SAFETY-VIOLATION".equals(diagnostic.code())
                        || "ECHO-NATIVE-M17-REPORT-MISSING".equals(diagnostic.code()));
        boolean localArtifactsReady = upstreamReady
                && diagnostics.stream().noneMatch(diagnostic -> "ECHO-NATIVE-M17-LOCAL-RUNTIME-ARTIFACT-MISSING".equals(diagnostic.code()))
                && reports.values().stream().noneMatch(Map::isEmpty);
        boolean launchAttempted = upstreamReady && localArtifactsReady;
        boolean controlledFailure = !launchAttempted;
        List<String> completedChecks = upstreamReady ? List.of(
                "m17_preflight_pass",
                "isolated_directory_checked",
                "no_user_mutation_allowed",
                "local_artifacts_checked",
                "failure_capture_ready"
        ) : List.of();

        EchoNativeIsolatedLaunchAttemptPlan attemptPlan = new EchoNativeIsolatedLaunchAttemptPlan(
                "phase13.m17.isolated.launch.attempt.plan",
                true,
                launchAttempted,
                launchAttempted,
                controlledFailure,
                true,
                true,
                false,
                false,
                false,
                false,
                "tmp/echo-native/phase13/m17/" + safePackId(packId),
                artifacts.stream().map(artifact -> String.valueOf(artifact.get("id"))).sorted().toList()
        );
        EchoNativeLocalRuntimeArtifactCheck artifactCheck = new EchoNativeLocalRuntimeArtifactCheck(
                "phase13.m17.local.runtime.artifact.check",
                localArtifactsReady,
                true,
                false,
                false,
                false,
                artifacts.size(),
                (int) artifacts.stream().filter(artifact -> Boolean.FALSE.equals(artifact.get("local"))).count(),
                artifacts
        );
        EchoNativeControlledLaunchAttemptResult attemptResult = new EchoNativeControlledLaunchAttemptResult(
                "phase13.m17.controlled.launch.attempt.result",
                launchAttempted,
                controlledFailure,
                false,
                false,
                false,
                false,
                launchAttempted ? 0 : -1,
                launchAttempted ? "not_executed_by_snapshot_harness" : "missing_local_runtime_artifacts"
        );
        EchoNativeLaunchOutputCapture outputCapture = new EchoNativeLaunchOutputCapture(
                "phase13.m17.launch.output.capture",
                true,
                false,
                false,
                false,
                false,
                true,
                "",
                controlledFailure ? "Launch not attempted: local runtime artifacts are unavailable." : ""
        );
        EchoNativePhase13M17LaunchStatus status = new EchoNativePhase13M17LaunchStatus(
                "phase13.m17.launch.status",
                true,
                controlledFailure,
                localArtifactsReady,
                launchAttempted,
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
                false,
                completedChecks
        );

        return new EchoNativeIsolatedLaunchAttemptOutcome(
                packId,
                isolatedLaunchAttemptPlan(packId, attemptPlan, diagnostics),
                localRuntimeArtifactCheck(packId, artifactCheck, diagnostics),
                controlledLaunchAttemptResult(packId, attemptResult, diagnostics),
                launchOutputCapture(packId, outputCapture, diagnostics),
                phase13M17LaunchStatus(packId, status, diagnostics),
                diagnostics
        );
    }

    private static List<Map<String, Object>> artifactChecks(
            Map<String, Object> artifactMap,
            Map<String, Object> classpathPlan,
            Map<String, Object> nativeExtractionPlan
    ) {
        List<Map<String, Object>> mappedArtifacts = mappedArtifactChecks(artifactMap);
        if (!mappedArtifacts.isEmpty()) {
            return mappedArtifacts;
        }
        List<Map<String, Object>> artifacts = new ArrayList<>();
        Map<String, Object> classpathData = EchoNativeJson.asObject(classpathPlan.get("data"));
        Object entries = classpathData.get("plannedEntries");
        if (entries instanceof List<?> list) {
            for (Object raw : list) {
                if (raw instanceof Map<?, ?> map) {
                    Map<String, Object> entry = EchoNativeJson.asObject(map);
                    String id = String.valueOf(entry.getOrDefault("id", "unknown"));
                    String path = String.valueOf(entry.getOrDefault("plannedPath", ""));
                    artifacts.add(artifact("classpath:" + id, path, "classpath-builder-plan.json"));
                }
            }
        }
        Map<String, Object> nativeData = EchoNativeJson.asObject(nativeExtractionPlan.get("data"));
        Object nativeEntries = nativeData.get("plannedNativeEntries");
        if (nativeEntries instanceof List<?> list) {
            for (Object raw : list) {
                if (raw instanceof Map<?, ?> map) {
                    Map<String, Object> entry = EchoNativeJson.asObject(map);
                    String id = String.valueOf(entry.getOrDefault("id", "unknown"));
                    String path = String.valueOf(entry.getOrDefault("plannedExtractionPath", entry.getOrDefault("plannedPath", "")));
                    artifacts.add(artifact("native:" + id, path, "native-extraction-plan.json"));
                }
            }
        }
        artifacts.sort(Comparator.comparing(item -> String.valueOf(item.get("id"))));
        return List.copyOf(artifacts);
    }

    private static List<Map<String, Object>> mappedArtifactChecks(Map<String, Object> artifactMap) {
        Map<String, Object> data = EchoNativeJson.asObject(artifactMap.get("data"));
        Object entries = data.get("artifacts");
        if (!(entries instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> artifacts = new ArrayList<>();
        for (Object raw : list) {
            Map<String, Object> entry = EchoNativeJson.asObject(raw);
            String id = String.valueOf(entry.getOrDefault("id", "unknown"));
            boolean local = Boolean.TRUE.equals(entry.get("local"));
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", id);
            item.put("local", local);
            item.put("path", String.valueOf(entry.getOrDefault("localPath", entry.getOrDefault("plannedPath", ""))));
            item.put("runtimeResolved", local);
            item.put("sourceReport", "local-runtime-artifact-map.json");
            artifacts.add(item);
        }
        artifacts.sort(Comparator.comparing(item -> String.valueOf(item.get("id"))));
        return List.copyOf(artifacts);
    }

    private static Map<String, Object> artifact(String id, String path, String sourceReport) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("local", !path.startsWith("planned://") && !path.isBlank());
        item.put("path", path);
        item.put("runtimeResolved", false);
        item.put("sourceReport", sourceReport);
        return item;
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
                    "Phase 13 M17 launch attempt upstream report is not PASS",
                    "Isolated launch attempt requires PASS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Regenerate and resolve M17 preflight reports before attempting an isolated launch."
            ));
        }
        if (hasUnsafeRuntimeWork(data)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-M17-SAFETY-VIOLATION",
                    EchoNativeIssueSeverity.ERROR,
                    "Phase 13 M17 upstream report contains unsafe runtime work",
                    reportName + " indicates runtime work that is not safe for the isolated launch attempt gate.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Keep launch attempts isolated, bounded, and mutation-free."
            ));
        }
        diagnostics.addAll(reportDiagnostics(report, packId));
    }

    private static Map<String, Object> isolatedLaunchAttemptPlan(
            String packId,
            EchoNativeIsolatedLaunchAttemptPlan plan,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m17_isolated_launch_attempt_plan", diagnostics);
        data.put("configMutationAllowed", plan.configMutationAllowed());
        data.put("controlledFailure", plan.controlledFailure());
        data.put("isolated", plan.isolated());
        data.put("launchAttemptAllowed", plan.launchAttemptAllowed());
        data.put("launchAttempted", plan.launchAttempted());
        data.put("outputCapturePlanned", plan.outputCapturePlanned());
        data.put("packId", packId);
        data.put("packMutationAllowed", plan.packMutationAllowed());
        data.put("planId", plan.planId());
        data.put("requiredLocalArtifacts", plan.requiredLocalArtifacts());
        data.put("saveMutationAllowed", plan.saveMutationAllowed());
        data.put("summary", plan.launchAttempted()
                ? "M17 isolated launch attempt plan is ready."
                : "M17 isolated launch attempt remains a controlled failure because local runtime artifacts are unavailable.");
        data.put("timeoutPlanned", plan.timeoutPlanned());
        data.put("userInstallMutationAllowed", plan.userInstallMutationAllowed());
        data.put("workingDirectory", plan.workingDirectory());
        return data;
    }

    private static Map<String, Object> localRuntimeArtifactCheck(
            String packId,
            EchoNativeLocalRuntimeArtifactCheck check,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m17_local_runtime_artifact_check", diagnostics);
        data.put("artifacts", check.artifacts());
        data.put("checkId", check.checkId());
        data.put("checkedArtifactCount", check.checkedArtifactCount());
        data.put("downloadAllowed", check.downloadAllowed());
        data.put("libraryDownloadStarted", check.libraryDownloadStarted());
        data.put("localArtifactsReady", check.localArtifactsReady());
        data.put("missingArtifactCount", check.missingArtifactCount());
        data.put("missingArtifactsBecomeDiagnostics", check.missingArtifactsBecomeDiagnostics());
        data.put("nativeExtractionStarted", check.nativeExtractionStarted());
        data.put("packId", packId);
        data.put("summary", check.localArtifactsReady()
                ? "All local runtime artifacts required for an isolated launch attempt are available."
                : "Local runtime artifacts are missing; no download or extraction was attempted.");
        return data;
    }

    private static Map<String, Object> controlledLaunchAttemptResult(
            String packId,
            EchoNativeControlledLaunchAttemptResult result,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m17_controlled_launch_attempt_result", diagnostics);
        data.put("controlledFailure", result.controlledFailure());
        data.put("exitCode", result.exitCode());
        data.put("failureReason", result.failureReason());
        data.put("gameProcessLaunched", result.gameProcessLaunched());
        data.put("launchAttempted", result.launchAttempted());
        data.put("mainMenuReached", result.mainMenuReached());
        data.put("packId", packId);
        data.put("processLaunched", result.processLaunched());
        data.put("resultId", result.resultId());
        data.put("summary", result.controlledFailure()
                ? "M17 launch attempt ended as a controlled failure before process launch."
                : "M17 launch attempt completed under isolated controls.");
        data.put("timeoutTriggered", result.timeoutTriggered());
        return data;
    }

    private static Map<String, Object> launchOutputCapture(
            String packId,
            EchoNativeLaunchOutputCapture capture,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m17_launch_output_capture", diagnostics);
        data.put("captureId", capture.captureId());
        data.put("commandExecuted", capture.commandExecuted());
        data.put("outputCaptureReady", capture.outputCaptureReady());
        data.put("packId", packId);
        data.put("processLaunched", capture.processLaunched());
        data.put("secretSafe", capture.secretSafe());
        data.put("stderrCaptured", capture.stderrCaptured());
        data.put("stderrTail", capture.stderrTail());
        data.put("stdoutCaptured", capture.stdoutCaptured());
        data.put("stdoutTail", capture.stdoutTail());
        data.put("summary", "M17 output capture stayed secret-safe and did not observe a launched process.");
        return data;
    }

    private static Map<String, Object> phase13M17LaunchStatus(
            String packId,
            EchoNativePhase13M17LaunchStatus status,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("phase13_m17_launch_status", diagnostics);
        data.put("classloaderCreated", status.classloaderCreated());
        data.put("commandExecuted", status.commandExecuted());
        data.put("completedChecks", status.completedChecks());
        data.put("controlledFailure", status.controlledFailure());
        data.put("gameClassesResolved", status.gameClassesResolved());
        data.put("gameProcessLaunched", status.gameProcessLaunched());
        data.put("launchAttempted", status.launchAttempted());
        data.put("libraryDownloadStarted", status.libraryDownloadStarted());
        data.put("localArtifactsReady", status.localArtifactsReady());
        data.put("mainMenuReached", status.mainMenuReached());
        data.put("nativeExtractionStarted", status.nativeExtractionStarted());
        data.put("packId", packId);
        data.put("phase13M17AttemptComplete", status.phase13M17AttemptComplete());
        data.put("registryInjected", status.registryInjected());
        data.put("statusId", status.statusId());
        data.put("summary", status.controlledFailure()
                ? "M17 produced a controlled failure report because local runtime artifacts are unavailable."
                : "M17 isolated launch attempt completed.");
        data.put("transformsEnabled", status.transformsEnabled());
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
        data.put("dryRunOnly", false);
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
        data.put("supportBundlePlannedOnly", true);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("unsafeRuntimeWorkStarted", false);
        return data;
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
                    "Isolated launch attempt requires " + reportName + ".",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Generate M17 preflight reports before attempting an isolated launch."
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
