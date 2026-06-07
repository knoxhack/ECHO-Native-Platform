package dev.echo.nativeplatform.cli;

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

final class EchoNativeLaunchRealityAuditor {
    EchoNativeLaunchRealityAuditOutcome audit(
            String packId,
            Path fixture,
            Map<String, Path> requiredReports
    ) throws IOException {
        List<EchoNativeDiagnostic> diagnostics = new ArrayList<>();
        Map<String, Map<String, Object>> reports = new LinkedHashMap<>();
        for (Map.Entry<String, Path> entry : requiredReports.entrySet()) {
            Map<String, Object> report = readRequiredReport(entry.getValue(), fixture, packId, entry.getKey(), diagnostics);
            reports.put(entry.getKey(), report);
            checkReport(entry.getKey(), entry.getValue(), report, packId, diagnostics);
        }

        Map<String, Object> launchAttempt = data(reports.get("controlled-launch-attempt-result.json"));
        Map<String, Object> launchStatus = data(reports.get("phase13-m17-launch-status.json"));
        Map<String, Object> smokeResult = data(reports.get("smoke-session-result.json"));
        Map<String, Object> smokeCompletion = data(reports.get("phase13-m18-completion.json"));
        Map<String, Object> firstPlaytestGate = data(reports.get("first-playtest-open-gate.json"));

        boolean launchAttempted = bool(launchAttempt, "launchAttempted") || bool(launchStatus, "launchAttempted");
        boolean processLaunched = bool(launchAttempt, "processLaunched") || bool(launchStatus, "processLaunched") || bool(smokeResult, "processLaunched");
        boolean gameProcessLaunched = bool(launchAttempt, "gameProcessLaunched") || bool(launchStatus, "gameProcessLaunched") || bool(smokeResult, "gameProcessLaunched");
        boolean minecraftLaunched = bool(launchAttempt, "minecraftLaunched") || bool(launchStatus, "minecraftLaunched") || bool(smokeResult, "minecraftLaunched");
        boolean mainMenuReached = bool(launchAttempt, "mainMenuReached") || bool(launchStatus, "mainMenuReached") || bool(smokeResult, "mainMenuReached");
        boolean smokeSessionComplete = bool(smokeCompletion, "phase13M18Complete");
        boolean firstPlaytestOpen = bool(firstPlaytestGate, "firstPlaytestOpen");
        boolean harnessOnlyLaunchAttempt = launchAttempted && !processLaunched && !gameProcessLaunched && !minecraftLaunched && !mainMenuReached;
        boolean realMinecraftLaunchImplemented = processLaunched && gameProcessLaunched && minecraftLaunched;
        boolean nativeLoaderBetaReady = realMinecraftLaunchImplemented && smokeSessionComplete && firstPlaytestOpen;

        if (diagnostics.stream().noneMatch(EchoNativeLaunchRealityAuditor::isBlocking) && harnessOnlyLaunchAttempt) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-LAUNCH-REALITY-HARNESS-ONLY",
                    EchoNativeIssueSeverity.WARNING,
                    "Native loader launch attempt is still harness-only",
                    "The current isolated launch attempt reports launchAttempted=true, but processLaunched=false, gameProcessLaunched=false, minecraftLaunched=false, and mainMenuReached=false.",
                    null,
                    packId,
                    List.of("reports/echo-native/" + packId + "/controlled-launch-attempt-result.json", "reports/echo-native/" + packId + "/phase13-m17-launch-status.json"),
                    "Implement the isolated runtime workspace and real process launch harness before calling this a tester-startable native loader beta."
            ));
        }

        List<EchoNativeDiagnostic> sortedDiagnostics = diagnostics.stream()
                .sorted(Comparator.comparing(EchoNativeDiagnostic::code)
                        .thenComparing(diagnostic -> diagnostic.moduleId() == null ? "" : diagnostic.moduleId())
                        .thenComparing(EchoNativeDiagnostic::summary))
                .toList();
        boolean phase1Complete = sortedDiagnostics.stream().noneMatch(EchoNativeLaunchRealityAuditor::isBlocking);
        List<Map<String, Object>> classifications = commandClassifications(harnessOnlyLaunchAttempt, realMinecraftLaunchImplemented);
        List<Map<String, Object>> actions = nextActions(realMinecraftLaunchImplemented, nativeLoaderBetaReady);

        return new EchoNativeLaunchRealityAuditOutcome(
                packId,
                realityAudit(packId, fixture, phase1Complete, launchAttempted, processLaunched, gameProcessLaunched, minecraftLaunched, mainMenuReached, harnessOnlyLaunchAttempt, realMinecraftLaunchImplemented, nativeLoaderBetaReady, firstPlaytestOpen, sortedDiagnostics),
                commandClassification(packId, classifications, harnessOnlyLaunchAttempt, realMinecraftLaunchImplemented, sortedDiagnostics),
                nextActions(packId, actions, nativeLoaderBetaReady, sortedDiagnostics),
                sortedDiagnostics
        );
    }

    private static Map<String, Object> realityAudit(
            String packId,
            Path fixture,
            boolean phase1Complete,
            boolean launchAttempted,
            boolean processLaunched,
            boolean gameProcessLaunched,
            boolean minecraftLaunched,
            boolean mainMenuReached,
            boolean harnessOnlyLaunchAttempt,
            boolean realMinecraftLaunchImplemented,
            boolean nativeLoaderBetaReady,
            boolean firstPlaytestOpen,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("native_loader_reality_audit", diagnostics);
        data.put("fixture", fixture.toString().replace('\\', '/'));
        data.put("firstPlaytestOpen", firstPlaytestOpen);
        data.put("harnessOnlyLaunchAttempt", harnessOnlyLaunchAttempt);
        data.put("launchAttempted", launchAttempted);
        data.put("mainMenuReached", mainMenuReached);
        data.put("nativeLoaderBetaReady", nativeLoaderBetaReady);
        data.put("nextImplementationPhase", realMinecraftLaunchImplemented ? "phase7.ashfall_module_runtime_enablement" : "phase2.isolated_runtime_workspace");
        data.put("packId", packId);
        data.put("phase1Complete", phase1Complete);
        data.put("processLaunched", processLaunched);
        data.put("gameProcessLaunched", gameProcessLaunched);
        data.put("minecraftLaunched", minecraftLaunched);
        data.put("realMinecraftLaunchImplemented", realMinecraftLaunchImplemented);
        data.put("testerStartable", nativeLoaderBetaReady);
        data.put("summary", nativeLoaderBetaReady
                ? "Native loader beta is tester-startable from a real isolated Minecraft process."
                : "Native loader beta is not tester-startable yet; current launch evidence is harness-only or blocked.");
        return data;
    }

    private static Map<String, Object> commandClassification(
            String packId,
            List<Map<String, Object>> classifications,
            boolean harnessOnlyLaunchAttempt,
            boolean realMinecraftLaunchImplemented,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("native_loader_launch_command_classification", diagnostics);
        data.put("classificationCount", classifications.size());
        data.put("commands", classifications);
        data.put("harnessOnlyLaunchAttempt", harnessOnlyLaunchAttempt);
        data.put("nativeLoaderBetaReady", false);
        data.put("packId", packId);
        data.put("realMinecraftLaunchImplemented", realMinecraftLaunchImplemented);
        data.put("summary", realMinecraftLaunchImplemented
                ? "At least one command is classified as a real isolated Minecraft launch."
                : "No command is currently classified as a real Minecraft launch.");
        return data;
    }

    private static Map<String, Object> nextActions(
            String packId,
            List<Map<String, Object>> actions,
            boolean nativeLoaderBetaReady,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        Map<String, Object> data = base("native_loader_beta_implementation_next_actions", diagnostics);
        data.put("actionCount", actions.size());
        data.put("actions", actions);
        data.put("nativeLoaderBetaReady", nativeLoaderBetaReady);
        data.put("packId", packId);
        data.put("summary", nativeLoaderBetaReady
                ? "No native loader beta implementation actions remain in this audit."
                : "Native loader beta implementation still needs a real isolated process launcher before tester use.");
        return data;
    }

    private static List<Map<String, Object>> commandClassifications(boolean harnessOnlyLaunchAttempt, boolean realMinecraftLaunchImplemented) {
        List<Map<String, Object>> commands = new ArrayList<>();
        commands.add(command("phase13 launch preflight <fixture>", "report_only_gate", false, false, "Verifies launch readiness reports without starting a process."));
        commands.add(command("phase13 run dummy-process <fixture>", "retired_internal_qa", false, false, "Retired from the product CLI; the inert dummy boundary is QA-only."));
        commands.add(command("phase13 launch attempt --isolated <fixture>", harnessOnlyLaunchAttempt ? "harness_only_no_process" : "isolated_launch_evidence", !harnessOnlyLaunchAttempt && realMinecraftLaunchImplemented, realMinecraftLaunchImplemented, harnessOnlyLaunchAttempt ? "Currently writes launch-attempt reports without spawning Minecraft." : "Current reports indicate a real isolated launch path."));
        commands.add(command("phase13 verify m18 <fixture>", "smoke_session_report_gate", false, false, "Verifies smoke-session reports and safety gates."));
        commands.add(command("phase13 package first-playtest <fixture>", "candidate_packaging_report", false, false, "Packages tester-facing reports and labels, not a game process."));
        commands.sort(Comparator.comparing(item -> String.valueOf(item.get("command"))));
        return List.copyOf(commands);
    }

    private static Map<String, Object> command(String command, String classification, boolean processCapable, boolean realMinecraftLaunch, String summary) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("classification", classification);
        item.put("command", command);
        item.put("downloadsAllowed", false);
        item.put("mutatesUserInstall", false);
        item.put("processCapable", processCapable);
        item.put("realMinecraftLaunch", realMinecraftLaunch);
        item.put("summary", summary);
        return item;
    }

    private static List<Map<String, Object>> nextActions(boolean realMinecraftLaunchImplemented, boolean nativeLoaderBetaReady) {
        if (nativeLoaderBetaReady) {
            return List.of();
        }
        List<Map<String, Object>> actions = new ArrayList<>();
        if (!realMinecraftLaunchImplemented) {
            actions.add(action(
                    "phase2.isolated_runtime_workspace",
                    "Create isolated runtime workspace",
                    "Materialize only fixture-local working, logs, and natives directories with no user cache/install mutation.",
                    List.of("echo-native-platform/fixtures/ashfall/local-runtime", "echo-native-platform/reports/echo-native/ashfall/native-loader-reality-audit.json")
            ));
            actions.add(action(
                    "phase3.real_process_launch_harness",
                    "Implement controlled process launch harness",
                    "Add an opt-in command that starts Minecraft from fixture-local artifacts with timeout, output capture, and mutation guards.",
                    List.of("echo-native-platform/echo-native-cli/src/main/java/dev/echo/nativeplatform/cli")
            ));
            actions.add(action(
                    "phase4.minecraft_main_class_args_resolution",
                    "Resolve real main class and launch args",
                    "Convert launch planning reports into a validated command line without downloading or touching user installs.",
                    List.of("echo-native-platform/reports/echo-native/ashfall/launch-argument-builder-plan.json")
            ));
        }
        return actions.stream()
                .sorted(Comparator.comparing(item -> String.valueOf(item.get("id"))))
                .toList();
    }

    private static Map<String, Object> action(String id, String title, String summary, List<String> files) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("title", title);
        item.put("summary", summary);
        item.put("files", files);
        item.put("commandExecuted", false);
        item.put("downloadsAllowed", false);
        item.put("filesystemMutated", false);
        item.put("processLaunched", false);
        return item;
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
                    "ECHO-NATIVE-LAUNCH-REALITY-REPORT-MISSING",
                    EchoNativeIssueSeverity.ERROR,
                    "Launch reality audit required report missing",
                    "Launch reality audit requires " + reportName + ".",
                    null,
                    packId,
                    List.of(fixture.toString().replace('\\', '/')),
                    "Regenerate Phase 13 launch, smoke, and first-playtest reports before auditing beta reality."
            ));
            return Map.of();
        }
        return EchoNativeJson.asObject(EchoNativeJson.parse(Files.readString(reportPath)));
    }

    private static void checkReport(
            String reportName,
            Path path,
            Map<String, Object> report,
            String packId,
            List<EchoNativeDiagnostic> diagnostics
    ) {
        if (report.isEmpty()) {
            return;
        }
        String status = String.valueOf(report.getOrDefault("status", "MISSING"));
        if (!"PASS".equals(status) && !"PASS_WITH_WARNINGS".equals(status)) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-LAUNCH-REALITY-UPSTREAM-BLOCKED",
                    EchoNativeIssueSeverity.ERROR,
                    "Launch reality audit upstream report is not PASS",
                    "Launch reality audit requires PASS or accepted PASS_WITH_WARNINGS " + reportName + ".",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Resolve upstream launch/smoke/first-playtest reports before beta readiness work."
            ));
        }
        if (hasUnsafeRuntimeWork(data(report))) {
            diagnostics.add(new EchoNativeDiagnostic(
                    "ECHO-NATIVE-LAUNCH-REALITY-UNSAFE-WORK",
                    EchoNativeIssueSeverity.ERROR,
                    "Launch reality audit found unsafe runtime work",
                    reportName + " indicates mutation, download, transform, classloading, or runtime process work outside the allowed launch path.",
                    null,
                    packId,
                    List.of(relativeReportPath(path)),
                    "Keep all launch work isolated, explicit, and report-backed."
            ));
        }
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
        data.put("downloadsAllowed", false);
        data.put("filesystemMutated", false);
        data.put("gameClassesResolved", false);
        data.put("gameProcessLaunched", false);
        data.put("libraryDownloadStarted", false);
        data.put("minecraftLaunched", false);
        data.put("nativeExtractionStarted", false);
        data.put("packMutationAllowed", false);
        data.put("phase", phase);
        data.put("processLaunched", false);
        data.put("registryInjected", false);
        data.put("registryMutated", false);
        data.put("reportOnly", true);
        data.put("saveMutationAllowed", false);
        data.put("transformsEnabled", false);
        data.put("transformsPerformed", false);
        data.put("unsafeRuntimeWorkStarted", false);
        data.put("userInstallMutationAllowed", false);
        return data;
    }

    private static Map<String, Object> data(Map<String, Object> report) {
        return EchoNativeJson.asObject(report == null ? null : report.get("data"));
    }

    private static boolean bool(Map<String, Object> data, String key) {
        return Boolean.TRUE.equals(data.get(key));
    }

    private static boolean hasUnsafeRuntimeWork(Map<String, Object> data) {
        return Boolean.TRUE.equals(data.get("remoteManifestDownloaded"))
                || Boolean.TRUE.equals(data.get("libraryDownloadStarted"))
                || Boolean.TRUE.equals(data.get("cacheMutated"))
                || Boolean.TRUE.equals(data.get("nativeExtractionStarted"))
                || Boolean.TRUE.equals(data.get("nativeFilesExtracted"))
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
                || Boolean.TRUE.equals(data.get("unsafeRuntimeWorkStarted"));
    }

    private static boolean isBlocking(EchoNativeDiagnostic diagnostic) {
        return diagnostic.severity() == EchoNativeIssueSeverity.ERROR
                || diagnostic.severity() == EchoNativeIssueSeverity.FATAL;
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
